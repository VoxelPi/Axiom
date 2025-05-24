package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.instruction.Condition

public sealed interface ScopeJumpStatement : ConditionalStatement {

    public val scope: ScopeLike

    @StatementType("exit")
    public data class Exit(
        override val condition: Condition,
        override val conditionValue: RegisterLike,
        override val scope: ScopeLike,
    ) : ScopeJumpStatement

    @StatementType("repeat")
    public data class Repeat(
        override val condition: Condition,
        override val conditionValue: RegisterLike,
        override val scope: ScopeLike,
    ) : ScopeJumpStatement
}
