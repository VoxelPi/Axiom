package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken

internal interface TokenMapping : TokenTransformation {

    fun map(token: LexerToken): LexerToken

    override fun transform(tokens: List<LexerToken>): List<LexerToken> {
        return tokens.map(::map)
    }
}
