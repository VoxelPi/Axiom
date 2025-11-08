package net.voxelpi.axiom.asm.compositor

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken

internal object Compositor {

    fun composite(rootUnit: List<LexerToken>, unitProvider: (id: String) -> List<LexerToken>?) {}
}
