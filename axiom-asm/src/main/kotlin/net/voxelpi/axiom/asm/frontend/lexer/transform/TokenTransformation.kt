package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken

public interface TokenTransformation {

    public fun transform(tokens: List<LexerToken>): List<LexerToken>
}
