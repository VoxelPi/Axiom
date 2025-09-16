package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.Token

internal object StrongSeparatorTokenMapping : TokenMapping {

    override fun map(token: Token): Token {
        return if (token is Token.Symbol && token.symbol == ";") {
            Token.Separator.Strong(token.source)
        } else {
            token
        }
    }
}
