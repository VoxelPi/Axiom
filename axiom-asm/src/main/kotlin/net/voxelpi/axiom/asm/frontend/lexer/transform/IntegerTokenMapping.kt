package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken

/**
 * Generates integer tokens from text.
 */
internal object IntegerTokenMapping : TokenMapping {

    override fun map(token: LexerToken): LexerToken {
        if (token !is LexerToken.Symbol) {
            return token
        }

        // Base-16 integer.
        if (INTEGER_PATTERN_B16.matches(token.symbol)) {
            val integerText = token.symbol.substring(2).replace("_", "")
            val integer = integerText.toLong(16)
            return LexerToken.Integer(integer, token.source)
        }

        // Base-10 integer.
        if (INTEGER_PATTERN_B10.matches(token.symbol)) {
            val integerText = token.symbol.substring(2).replace("_", "")
            val integer = integerText.toLong(10)
            return LexerToken.Integer(integer, token.source)
        }

        // Base-8 integer.
        if (INTEGER_PATTERN_B8.matches(token.symbol)) {
            val integerText = token.symbol.substring(2).replace("_", "")
            val integer = integerText.toLong(8)
            return LexerToken.Integer(integer, token.source)
        }

        // Base-2 integer.
        if (INTEGER_PATTERN_B2.matches(token.symbol)) {
            val integerText = token.symbol.substring(2).replace("_", "")
            val integer = integerText.toLong(2)
            return LexerToken.Integer(integer, token.source)
        }

        // Base-10 integer (implicit).
        if (INTEGER_PATTERN_B10_IMPLICIT.matches(token.symbol)) {
            val integerText = token.symbol.replace("_", "")
            val integer = integerText.toLong(10)
            return LexerToken.Integer(integer, token.source)
        }

        return token
    }

    private val INTEGER_PATTERN_B16 = "^(0[xX][0-9a-fA-F_]+)".toRegex()
    private val INTEGER_PATTERN_B10 = "^(0[dD]?[0-9_]+)".toRegex()
    private val INTEGER_PATTERN_B8 = "^(0[oO][0-7_]+)".toRegex()
    private val INTEGER_PATTERN_B2 = "^(0[bB][01_]+)".toRegex()
    private val INTEGER_PATTERN_B10_IMPLICIT = "^([0-9][0-9_]*)".toRegex()
}
