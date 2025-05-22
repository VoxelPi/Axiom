package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.instruction.Condition

public data class ExitStatement(
    override val source: SourceLink,
    val scope: StatementArgument<ScopeLike>,
    val condition: StatementArgument<Condition>,
    val conditionRegister: StatementArgument<RegisterLike>,
) : Statement
