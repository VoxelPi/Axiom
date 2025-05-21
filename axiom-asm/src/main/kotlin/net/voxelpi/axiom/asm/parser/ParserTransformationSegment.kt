package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.Token

public interface ParserTransformationSegment {
    public fun isApplicable(token: Token): Boolean
}
