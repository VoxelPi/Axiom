package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.source.join

internal object NamespacedIdTransformation : TokenTransformation {

    override fun transform(tokens: List<LexerToken>): List<LexerToken> {
        val transformedTokens = mutableListOf<LexerToken>()

        var iToken = 0
        while (iToken < tokens.size) {
            val token = tokens[iToken]
            if (token !is LexerToken.Text) {
                ++iToken
                transformedTokens.add(token)
                continue
            }
            ++iToken

            val symbols = mutableListOf(token)
            while (iToken + 2 < tokens.size) {
                val colon1Token = tokens[iToken + 0]
                val colon2Token = tokens[iToken + 1]
                val symbolToken = tokens[iToken + 2]

                if (colon1Token !is LexerToken.Symbol || colon1Token.symbol != ":") {
                    break
                }
                if (colon2Token !is LexerToken.Symbol || colon2Token.symbol != ":") {
                    break
                }
                if (symbolToken !is LexerToken.Text) {
                    break
                }
                iToken += 3
                symbols.add(symbolToken)
            }

            if (symbols.size == 1) {
                transformedTokens.add(token)
                continue
            }

            val namespaceIdToken = LexerToken.Text(
                symbols.joinToString("::") { it.value },
                symbols.map { it.source }.join(),
            )
            transformedTokens.add(namespaceIdToken)
        }

        return transformedTokens
    }
}
