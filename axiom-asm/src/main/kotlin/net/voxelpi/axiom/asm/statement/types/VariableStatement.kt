package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.asm.type.VariableLike

public sealed interface VariableStatement {

    @StatementType("variable/definition")
    public data class Definition(
        val name: VariableLike.VariableName,
        val value: ValueLike,
    ) : LabelStatement
}
