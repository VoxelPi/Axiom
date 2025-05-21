package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.argument.Argument
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public sealed interface InstructionStatement : Statement {

    public val operation: Operation
    public val condition: Condition
    public val conditionRegister: Argument.RegisterLike
    public val inputA: Argument.ValueLike
    public val inputB: Argument.ValueLike

    public data class NoOutput(
        override val source: SourceLink,
        override val operation: Operation,
        override val condition: Condition,
        override val conditionRegister: Argument.RegisterLike,
        override val inputA: Argument.ValueLike,
        override val inputB: Argument.ValueLike,
    ) : InstructionStatement

    public data class WithOutput(
        override val source: SourceLink,
        override val operation: Operation,
        override val condition: Condition,
        override val conditionRegister: Argument.RegisterLike,
        override val inputA: Argument.ValueLike,
        override val inputB: Argument.ValueLike,
        public val output: Argument.RegisterLike,
    ) : InstructionStatement
}
