package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.instruction.Condition

@StatementType("if")
public data class IfStatement(
    public val condition: Condition,
    public val conditionValue: RegisterLike,
)
