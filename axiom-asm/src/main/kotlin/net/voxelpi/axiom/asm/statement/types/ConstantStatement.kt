package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.ValueLike

@StatementType("constant")
public data class ConstantStatement(
    val value: ValueLike,
) : ProgramElementStatement
