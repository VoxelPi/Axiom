package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.Token

internal interface TokenMapping : TokenTransformation {

    fun map(token: Token): Token

    override fun transform(tokens: List<Token>): List<Token> {
        return tokens.map(::map)
    }
}
