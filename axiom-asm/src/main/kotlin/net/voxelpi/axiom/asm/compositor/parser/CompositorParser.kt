package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.parser.TokenReader
import net.voxelpi.axiom.asm.frontend.parser.value.NamespacedIdParser
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.language.SeparatorType
import net.voxelpi.axiom.asm.source.SourcedValue
import net.voxelpi.axiom.asm.source.join
import net.voxelpi.axiom.asm.source.sourcedValue
import net.voxelpi.axiom.asm.token.isEmptyOrType
import net.voxelpi.axiom.asm.token.split
import net.voxelpi.axiom.asm.token.trim
import kotlin.reflect.typeOf

internal object CompositorParser {

    fun parse(lexerTokens: List<LexerToken>): List<CompositorToken> {
        val compositorTokens = mutableListOf<CompositorToken>()

        val reader = TokenReader(lexerTokens)
        while (reader.remaining() > 0) {
            val firstToken = reader.readToken() ?: break // Always non-null, as previously checked.
            if (firstToken !is LexerToken.Directive) {
                compositorTokens.add(transformSingleToken(firstToken))
                continue
            }

            val directive = firstToken.value
            val directiveToken = when (directive) {
                "include" -> {
                    reader.snapshot()

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Include directive is missing target.")

                    // Read unit id token.
                    val unitIdToken = reader.readToken()
                        ?: throw SourcedCompilationException(firstToken.source, "Include directive is missing target.")
                    val includedUnitId = when (unitIdToken) {
                        is LexerToken.StringLiteral -> DirectiveParameterValue.Value.of(unitIdToken.value, unitIdToken.source)
                        is LexerToken.Placeholder -> DirectiveParameterValue.Placeholder(String::class, sourcedValue(unitIdToken.id, unitIdToken.source))
                        else -> throw SourcedCompilationException(unitIdToken.source, "Invalid include target.")
                    }

                    // Calculate the source of the directive.
                    val directiveSource = reader.accept().map { it.source }.join()

                    CompositorToken.Directive.Include(directiveSource, includedUnitId)
                }
                "define" -> {
                    reader.snapshot()

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Definition has no id and value.")

                    // Parse the id of the definition.
                    val id = reader.readSourcedValue(NamespacedIdParser).getOrElse { exception ->
                        throw SourcedCompilationException(firstToken.source, "Invalid id for definition", exception)
                    }

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Definition ${id.value} has no value.")

                    // Parse content
                    val valueTokens = reader.readUntilSeparator(SeparatorType.NORMAL)
                    if (valueTokens.isEmpty()) {
                        throw SourcedCompilationException(firstToken.source, "Definition ${id.value} has no value.")
                    }

                    // Calculate the source of the directive.
                    val directiveSource = reader.accept().map { it.source }.join()

                    CompositorToken.Directive.Define(directiveSource, id.value, parse(valueTokens))
                }
                "insert" -> {
                    // !insert <template> (<param1>, <param2>, ...)
                    // !insert <placeholder> (<param1>, <param2>, ...)
                    // !insert (<arg1>, <arg2>, ...) -> { ... } (<param1>, <param2>, ...)

                    reader.snapshot()

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Insert has no template and parameters")

                    // Parse template header. Can be either a real header or a placeholder
                    val templateToken = reader.readToken()
                        ?: throw SourcedCompilationException(firstToken.source, "Insert has no template and parameters")
                    val templateHeader = when (templateToken) {
                        is LexerToken.Bracket if templateToken.type == BracketType.ROUND -> {
                            // Read separator.
                            reader.readSeparator(0..1)
                                ?: throw SourcedCompilationException(templateToken.source, "Invalid template header")

                            // Read arrow symbol.
                            reader.readSymbol("-")
                                ?: throw SourcedCompilationException(templateToken.source, "Invalid template header arrow")
                            reader.readSymbol(">")
                                ?: throw SourcedCompilationException(templateToken.source, "Invalid template header arrow")

                            val header = parseTemplateHeader(templateToken.tokens).getOrElse {
                                throw SourcedCompilationException(templateToken.source, "Invalid template header")
                            }
                            DirectiveParameterValue.Value(CompositorTemplateHeader::class, sourcedValue(header, templateToken.source))
                        }
                        is LexerToken.Placeholder -> DirectiveParameterValue.Placeholder(CompositorTemplateHeader::class, sourcedValue(templateToken.id, templateToken.source))
                        else -> throw SourcedCompilationException(templateToken.source, "Invalid template.")
                    }

                    // Parse separator.
                    reader.readSeparator(0..1)
                        ?: throw SourcedCompilationException(templateToken.source, "Missing template parameter values.")

                    // Parse template parameters.
                    val templateParametersToken = reader.readTypedTokenIf<LexerToken.Bracket> { it.type == BracketType.ROUND }
                        ?: throw SourcedCompilationException(templateToken.source, "Missing template parameter values.")
                    val prototype = parseTemplatePrototype(
                        templateParametersToken.tokens,
                    ).getOrElse { throw SourcedCompilationException(templateParametersToken.source, "Invalid template parameter values") }

                    CompositorToken.Directive.Insert(firstToken.source, templateHeader, prototype)
                }
                "region" -> CompositorToken.Directive.Region(firstToken.source)
                "at" -> {
                    reader.snapshot()

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "At directive is missing a position.")

                    // Read location token.
                    val positionToken = reader.readToken()
                        ?: throw SourcedCompilationException(firstToken.source, "At directive is missing a position.")
                    val position = when (positionToken) {
                        is LexerToken.Integer -> DirectiveParameterValue.Value.of(positionToken.value.toULong(), positionToken.source)
                        is LexerToken.Placeholder -> DirectiveParameterValue.Placeholder(ULong::class, sourcedValue(positionToken.id, positionToken.source))
                        else -> throw SourcedCompilationException(positionToken.source, "Invalid at position.")
                    }

                    // Calculate the source of the directive.
                    val directiveSource = reader.accept().map { it.source }.join()

                    CompositorToken.Directive.Location.At(directiveSource, position)
                }
                "in" -> {
                    reader.snapshot()

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "In directive is missing a region reference.")

                    // Read region reference token.
                    val regionIdToken = reader.readToken()
                        ?: throw SourcedCompilationException(firstToken.source, "In directive is missing a region reference.")
                    val regionId = when (regionIdToken) {
                        is LexerToken.Placeholder -> SourcedValue(regionIdToken.id, typeOf<NamespacedId>(), regionIdToken.source)
                        else -> throw SourcedCompilationException(regionIdToken.source, "Invalid region reference.")
                    }

                    // Calculate the source of the directive.
                    val directiveSource = reader.accept().map { it.source }.join()
                    CompositorToken.Directive.Location.In(directiveSource, regionId)
                }
                "if" -> {
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Missing condition.")

                    val conditionTokens = reader.readUntilSeparator(SeparatorType.NORMAL)
                    if (conditionTokens.isEmpty()) {
                        throw SourcedCompilationException(firstToken.source, "Missing condition.")
                    }

                    CompositorToken.Directive.If(firstToken.source, conditionTokens)
                }
                "else" -> CompositorToken.Directive.Else(firstToken.source)
                "repeated" -> {
                    reader.snapshot()

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Repeated directive is missing a repeat count.")

                    // Read repeat count.
                    val repeatCountToken = reader.readToken()
                        ?: throw SourcedCompilationException(firstToken.source, "Repeated directive is missing a repeat count.")
                    val repeatCount = when (repeatCountToken) {
                        is LexerToken.Integer -> DirectiveParameterValue.Value.of(repeatCountToken.value.toULong(), repeatCountToken.source)
                        is LexerToken.Placeholder -> DirectiveParameterValue.Placeholder(ULong::class, sourcedValue(repeatCountToken.id, repeatCountToken.source))
                        else -> throw SourcedCompilationException(repeatCountToken.source, "Invalid repeat count.")
                    }

                    // Calculate the source of the directive.
                    val directiveSource = reader.accept().map { it.source }.join()

                    CompositorToken.Directive.Repeated(directiveSource, repeatCount)
                }
                "inline" -> CompositorToken.Directive.Visibility.Inline(firstToken.source)
                "private" -> CompositorToken.Directive.Visibility.Private(firstToken.source)
                "public" -> {
                    reader.snapshot()

                    // Try parsing a separator.
                    if (reader.readSeparator(1) == null) {
                        reader.revert()
                        CompositorToken.Directive.Visibility.Public(firstToken.source, null)
                    } else {
                        val placeholderToken = reader.readTypedToken<LexerToken.Placeholder>()
                        if (placeholderToken != null) {
                            // Calculate the source of the directive.
                            val directiveSource = reader.accept().map { it.source }.join()

                            CompositorToken.Directive.Visibility.Public(directiveSource, sourcedValue(placeholderToken.id, placeholderToken.source))
                        } else {
                            reader.revert()
                            CompositorToken.Directive.Visibility.Public(firstToken.source, null)
                        }
                    }
                }
                "global" -> CompositorToken.Directive.Visibility.Global(firstToken.source)
                else -> throw SourcedCompilationException(firstToken.source, "Unknown directive \"${firstToken.value}\".")
            }
            compositorTokens.add(directiveToken)
        }

        return compositorTokens
    }

    private fun transformSingleToken(token: LexerToken): CompositorToken {
        return when (token) {
            is LexerToken.Bracket -> CompositorToken.Bracket(
                token.type,
                parse(token.tokens),
                token.openingBracketSource,
                token.closingBracketSource,
            )
            is LexerToken.Directive -> throw SourcedCompilationException(token.source, "Invalid directive ${token.value}, directive must be at the beginning of a statement.")
            is LexerToken.Label -> CompositorToken.Label(token.id, token.source)
            is LexerToken.Integer -> CompositorToken.Integer(token.value, token.source)
            is LexerToken.Placeholder -> CompositorToken.Placeholder(token.id, token.source)
            is LexerToken.Separator -> CompositorToken.Separator(token.type, token.source)
            is LexerToken.StringLiteral -> CompositorToken.StringLiteral(token.value, token.source)
            is LexerToken.Symbol -> CompositorToken.Symbol(token.symbol, token.source)
            is LexerToken.Text -> CompositorToken.Text(token.value, token.source)
        }
    }

    private fun parseTemplateHeader(tokens: List<LexerToken>): Result<CompositorTemplateHeader> {
        val parameters = mutableListOf<CompositorTemplateHeader.Parameter>()

        val parameterTokensList = tokens
            .split { it is LexerToken.Symbol && it.symbol == "," }
            .map { it.trim<LexerToken, LexerToken.Separator>() }
            .toMutableList()
        // Remove last parameter if empty.
        if (parameterTokensList.isNotEmpty()) {
            if (parameterTokensList.last().isEmptyOrType<LexerToken.Separator>()) {
                parameterTokensList.removeLast()
            }
        }

        for (parameterTokens in parameterTokensList) {
            val reader = TokenReader(parameterTokens)

            val parameterId = reader.readValue(NamespacedIdParser)
                .getOrElse { throw it }

            reader.snapshot()
            val separator1 = reader.readSeparator(0..1)
            val equalsSymbol = reader.readSymbol("=")
            if (separator1 == null || equalsSymbol == null) {
                parameters.add(CompositorTemplateHeader.Parameter(parameterId, null))
                continue
            }
            reader.readSeparator(0..1)
                ?: throw SourcedCompilationException(parameterTokens.map { it.source }.join(), "Invalid parameter default")
            val defaultTokens = reader.readRemainingTokens()
            if (defaultTokens.isEmpty()) {
                throw SourcedCompilationException(parameterTokens.map { it.source }.join(), "Invalid parameter default")
            }
            parameters.add(CompositorTemplateHeader.Parameter(parameterId, parse(defaultTokens)))
        }

        val header = CompositorTemplateHeader(parameters)
        return Result.success(header)
    }

    private fun parseTemplatePrototype(tokens: List<LexerToken>): Result<CompositorTemplatePrototype> {
        val positionParameters = mutableListOf<List<CompositorToken>>()
        val keywordParameters = mutableMapOf<NamespacedId, List<CompositorToken>>()

        val parameterTokensList = tokens
            .split { it is LexerToken.Symbol && it.symbol == "," }
            .map { it.trim<LexerToken, LexerToken.Separator>() }
            .toMutableList()
        // Remove last parameter if empty.
        if (parameterTokensList.isNotEmpty()) {
            if (parameterTokensList.last().isEmptyOrType<LexerToken.Separator>()) {
                parameterTokensList.removeLast()
            }
        }

        for (parameterTokens in parameterTokensList) {
            val reader = TokenReader(parameterTokens)

            // Check if argument is keyword argument.
            var isKeyword = true
            reader.snapshot()
            reader.skip(1) ?: { isKeyword = false }
            reader.readSymbol(":") ?: { isKeyword = false }
            reader.revert()

            if (isKeyword) {
                val id = reader.readSourcedValue(NamespacedIdParser)
                    .getOrElse { throw it }

                reader.readSeparator(0..1)
                    ?: throw SourcedCompilationException(id.source, "Invalid parameter id")
                reader.readSymbol(":")
                    ?: throw SourcedCompilationException(id.source, "Invalid parameter id")
                reader.readSeparator(0..1)
                    ?: throw SourcedCompilationException(id.source, "Invalid parameter id")

                val value = parse(reader.readRemainingTokens())
                keywordParameters[id.value] = value
            } else {
                val value = parse(reader.readRemainingTokens())
                positionParameters.add(value)
            }
        }

        val prototype = CompositorTemplatePrototype(positionParameters, keywordParameters)
        return Result.success(prototype)
    }
}
