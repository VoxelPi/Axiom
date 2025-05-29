package net.voxelpi.axiom.asm.lexer

import net.voxelpi.axiom.asm.source.SourceLink

public data class TokenizedStatement(
    val source: SourceLink,
    val tokens: List<Token>,
)
