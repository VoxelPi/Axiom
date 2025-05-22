package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public sealed interface InstructionStatement : Statement {

    public val operation: Operation
    public val condition: StatementArgument<Condition>
    public val conditionRegister: StatementArgument<RegisterLike>
    public val inputA: StatementArgument<ValueLike>
    public val inputB: StatementArgument<ValueLike>

    public data class NoOutput(
        override val source: SourceLink,
        override val operation: Operation,
        override val condition: StatementArgument<Condition>,
        override val conditionRegister: StatementArgument<RegisterLike>,
        override val inputA: StatementArgument<ValueLike>,
        override val inputB: StatementArgument<ValueLike>,
    ) : InstructionStatement

    public data class WithOutput(
        override val source: SourceLink,
        override val operation: Operation,
        override val condition: StatementArgument<Condition>,
        override val conditionRegister: StatementArgument<RegisterLike>,
        override val inputA: StatementArgument<ValueLike>,
        override val inputB: StatementArgument<ValueLike>,
        public val output: StatementArgument<RegisterLike>,
    ) : InstructionStatement
}
