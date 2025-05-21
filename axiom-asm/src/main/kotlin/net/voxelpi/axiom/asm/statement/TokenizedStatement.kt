package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.lexer.Token
import net.voxelpi.axiom.asm.source.SourceLink

public data class TokenizedStatement(
    val tokens: List<Token>,
) : Statement {

    override val source: SourceLink
        get() {
            if (tokens.isEmpty()) {
                return SourceLink.Generated("__empty__", "__empty__")
            }

            val firstTokenSource = tokens[0].source
            val length = tokens.sumOf {
                if (it.source.unit == firstTokenSource.unit) {
                    it.source.length
                } else {
                    0
                }
            }

            return SourceLink.CompilationUnitSlice(firstTokenSource.unit, firstTokenSource.index, firstTokenSource.line, firstTokenSource.column, length)
        }
}
