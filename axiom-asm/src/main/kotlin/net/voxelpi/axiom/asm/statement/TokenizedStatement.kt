package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.lexer.Token

public data class TokenizedStatement(
    val tokens: List<Token>,
) : Statement
