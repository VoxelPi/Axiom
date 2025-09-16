package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.Token
import net.voxelpi.axiom.asm.source.SourceReference
import kotlin.streams.toList

public object CharacterTokenTransformation : TokenTransformation {

    override fun transform(tokens: List<Token>): List<Token> {
        val transformedTokens = mutableListOf<Token>()

        var iToken = 0
        while (iToken < tokens.size) {
            val token = tokens[iToken]
            if (token !is Token.Symbol) {
                transformedTokens.add(token)
                iToken += 1
                continue
            }

            // Check if token is the opening of a char literal.
            if (token.symbol != "'") {
                transformedTokens.add(token)
                iToken += 1
                continue
            }
            val iOpeningQuote = iToken

            // Search the closing quote
            var iClosingQuote = iOpeningQuote + 1
            var escaped = false
            while (iClosingQuote < tokens.size) {
                val token = tokens[iClosingQuote]
                if (token !is Token.Symbol) {
                    iClosingQuote += 1
                    escaped = false
                    continue
                }
                when (token.symbol) {
                    "'" -> {
                        if (!escaped) {
                            break
                        }
                        escaped = false
                    }
                    "\\" -> {
                        escaped = !escaped
                    }
                    else -> {
                        escaped = false
                    }
                }
                iClosingQuote += 1
            }

            // Check if a closing quote can exist.
            if (iClosingQuote >= tokens.size) {
                throw SourcedCompilationException(token.source, "Unclosed single quote")
            }
            iToken = iClosingQuote + 1

            val openingSource = tokens[iOpeningQuote].source
            val closingSource = tokens[iClosingQuote].source
            if (openingSource.unit.id != closingSource.unit.id) {
                throw SourcedCompilationException(token.source, "Unclosed single quote (closing quote is in a different unit)")
            }

            val source = SourceReference.UnitSlice(openingSource.unit, openingSource.index, closingSource.index - openingSource.index + 1)
            val content = openingSource.unit.text.slice((openingSource.index + 1)..<closingSource.index)
            val contentCodePoints = content.codePoints().toList()

            // Handle empty characters.
            if (contentCodePoints.isEmpty()) {
                throw SourcedCompilationException(source, "Unclosed empty codepoints")
            }

            // Handle normal characters.
            if (contentCodePoints.size == 1) {
                transformedTokens.add(Token.Integer(contentCodePoints[0].toLong(), source))
                continue
            }

            // Handle escaped characters.
            if (contentCodePoints.size == 2 && content.startsWith('\\')) {
                val escapedChar = content[1]
                if (escapedChar !in ESCAPED_CHARACTER_MAPPING) {
                    throw SourcedCompilationException(source, "Invalid escaped character '$escapedChar'")
                }
                transformedTokens.add(Token.Integer(ESCAPED_CHARACTER_MAPPING[escapedChar]!!.code.toLong(), source))
                continue
            }

            // Everything else is invalid.
            throw SourcedCompilationException(source, "Invalid character literal '$content'")
        }

        return transformedTokens
    }

    private val ESCAPED_CHARACTER_MAPPING = mapOf(
        '0' to 0.toChar(),
        'n' to '\n',
        'r' to '\r',
        't' to '\t',
        'b' to '\b',
        'f' to 12.toChar(),
        'v' to 11.toChar(),
        '\'' to '\''
    )
}
