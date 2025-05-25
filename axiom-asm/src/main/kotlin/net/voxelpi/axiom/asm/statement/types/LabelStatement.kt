package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.LabelLike

public sealed interface LabelStatement {

    @StatementType("label/definition")
    public data class Definition(
        val name: LabelLike.LabelName,
    ) : LabelStatement
}
