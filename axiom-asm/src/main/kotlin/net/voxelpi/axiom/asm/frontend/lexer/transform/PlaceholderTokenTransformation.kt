package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.source.join

public object PlaceholderTokenTransformation : TokenTransformation {

    override fun transform(tokens: List<LexerToken>): List<LexerToken> {
        val transformedTokens = mutableListOf<LexerToken>()

        var iToken = 0
        while (iToken < tokens.size) {
            val token = tokens[iToken]
            if (token !is LexerToken.Symbol || token.symbol != "$") {
                ++iToken
                transformedTokens.add(token)
                continue
            }
            ++iToken

            // Check if variable symbol is the last symbol.
            if (iToken >= tokens.size) {
                throw SourcedCompilationException(token.source, "Missing variable name")
            }

            // Check that the variable name is a text token.
            val idToken = tokens[iToken]
            ++iToken
            if (idToken !is LexerToken.Text) {
                throw SourcedCompilationException(idToken.source, "Invalid variable name ${idToken.source}")
            }

            val variableName = idToken.value
            val variableId = NamespacedId(variableName.split("::"))

            transformedTokens.add(LexerToken.Placeholder(variableId, listOf(token.source, idToken.source).join()))
        }

        return transformedTokens
    }
}
