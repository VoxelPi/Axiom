package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.language.SeparatorType

internal object StrongSeparatorTokenMapping : TokenMapping {

    override fun map(token: LexerToken): LexerToken {
        return if (token is LexerToken.Symbol && token.symbol == ";") {
            LexerToken.Separator(SeparatorType.STRONG, token.source)
        } else {
            token
        }
    }
}
