package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.parser.TokenReader
import net.voxelpi.axiom.asm.frontend.parser.value.NamespacedIdParser
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.language.SeparatorType
import net.voxelpi.axiom.asm.source.SourcedValue
import net.voxelpi.axiom.asm.source.join
import net.voxelpi.axiom.asm.source.sourcedValue
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

                    CompositorToken.Directive.Define(firstToken.source, id.value, parse(valueTokens))
                }
                "insert" -> {
                    // !insert <template> (<param1>, <param2>, ...)
                    // !insert <placeholder> (<param1>, <param2>, ...)
                    // !insert (<arg1>, <arg2>, ...) -> { ... } (<param1>, <param2>, ...)

                    // Parse separator.
                    reader.readSeparator(1)
                        ?: throw SourcedCompilationException(firstToken.source, "Insert has no template and parameters")

                    CompositorToken.Directive.Insert(firstToken.source, TODO())
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
                "if" -> CompositorToken.Directive.If(firstToken.source, TODO())
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
}
