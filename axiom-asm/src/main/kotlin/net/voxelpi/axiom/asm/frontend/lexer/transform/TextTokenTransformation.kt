package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.language.SeparatorType
import net.voxelpi.axiom.asm.source.SourceReference

internal object TextTokenTransformation : TokenTransformation {

    override fun transform(tokens: List<LexerToken>): List<LexerToken> {
        val transformedTokens = mutableListOf<LexerToken>()

        var iToken = 0
        while (iToken < tokens.size) {
            // Wait for an opening quote.
            val token = tokens[iToken]
            if (token !is LexerToken.Symbol || token.symbol != "\"") {
                transformedTokens.add(token)
                iToken += 1
                continue
            }
            val iOpenToken = iToken
            val openToken = token
            ++iToken

            // Search closing token
            var escaped = false
            while (iToken < tokens.size) {
                when (val token = tokens[iToken]) {
                    is LexerToken.Symbol -> {
                        if (escaped) {
                            escaped = false
                        } else {
                            when (token.symbol) {
                                "\"" -> break
                                "\\" -> escaped = true
                            }
                        }
                    }
                    is LexerToken.Separator if token.type == SeparatorType.NORMAL -> throw SourcedCompilationException(token.source, "Invalid newline in string literal: $token")
                    is LexerToken.StringLiteral -> throw SourcedCompilationException(token.source, "Invalid token in string literal: $token")
                    else -> {
                        escaped = false
                    }
                }
                ++iToken
            }
            if (iToken >= tokens.size) {
                throw SourcedCompilationException(openToken.source, "String literal is not closed")
            }
            val iCloseToken = iToken
            val closeToken = tokens[iCloseToken]
            ++iToken

            // Extract content from source file.
            val source = SourceReference.UnitSlice(openToken.source.unit, openToken.source.index, closeToken.source.index - openToken.source.index + 1)
            var content = openToken.source.unit.text.slice((openToken.source.index + 1)..<closeToken.source.index)

            // Replace escaped characters.
            for ((escapedSymbol, replacement) in ESCAPED_SYMBOLS) {
                content = content.replace(escapedSymbol, replacement)
            }

            transformedTokens.add(LexerToken.StringLiteral(content, source))
        }

        return transformedTokens
    }

    private val ESCAPED_SYMBOLS = mapOf(
        "\\0" to 0.toChar().toString(),
        "\\n" to "\n",
        "\\r" to "\r",
        "\\t" to "\t",
        "\\b" to "\b",
        "\\f" to 12.toChar().toString(),
        "\\v" to 11.toChar().toString(),
        "\"" to "\""
    )
}
