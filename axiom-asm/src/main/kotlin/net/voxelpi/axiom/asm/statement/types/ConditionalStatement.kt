package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.instruction.Condition

public interface ConditionalStatement {
    public val condition: Condition
    public val conditionValue: RegisterLike
}
