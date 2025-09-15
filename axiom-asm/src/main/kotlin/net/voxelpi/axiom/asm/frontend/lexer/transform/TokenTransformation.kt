package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.Token

public interface TokenTransformation {

    public fun transform(tokens: List<Token>): List<Token>
}
