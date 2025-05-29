package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.Token

public data class ParserTransformationLiteral(
    val value: String,
) : ParserTransformationSegment {

    override fun isApplicable(token: Token): Boolean {
        return when (token) {
            is Token.Text -> token.value == value
            is Token.Integer -> token.source.text == value
            else -> false
        }
    }
}
