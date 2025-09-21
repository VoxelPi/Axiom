package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken

internal object TrimScopeTokenTransformation : TokenTransformation {

    override fun transform(tokens: List<LexerToken>): List<LexerToken> {
        val iFirstNonSeparatorToken = tokens.indexOfFirst { it !is LexerToken.Separator }
        val iLastNonSeparatorToken = tokens.indexOfLast { it !is LexerToken.Separator }

        // Check that the scope has any non-separator tokens. If not, we can early exit.
        if (iFirstNonSeparatorToken == -1 || iLastNonSeparatorToken == -1) {
            return tokens
        }

        // Trim separators.
        val trimmedTokens = tokens.subList(iFirstNonSeparatorToken, iLastNonSeparatorToken + 1)

        // Apply transformation to all child tokens.
        val transformedTokens = trimmedTokens.map {
            if (it is LexerToken.Bracket) {
                LexerToken.Bracket(
                    it.type,
                    transform(it.tokens),
                    it.openingBracketSource,
                    it.closingBracketSource,
                )
            } else {
                it
            }
        }

        return transformedTokens
    }
}
