package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public sealed interface InstructionStatement : ConditionalStatement, ProgramElementStatement {

    public val operation: Operation
    public val inputA: ValueLike
    public val inputB: ValueLike

    @StatementType("instruction/with_output")
    public class WithOutput(
        override val operation: Operation,
        override val condition: Condition,
        override val conditionValue: RegisterLike,
        override val inputA: ValueLike,
        override val inputB: ValueLike,
        public val output: RegisterLike,
    ) : InstructionStatement

    @StatementType("instruction/without_output")
    public class WithoutOutput(
        override val operation: Operation,
        override val condition: Condition,
        override val conditionValue: RegisterLike,
        override val inputA: ValueLike,
        override val inputB: ValueLike,
    ) : InstructionStatement
}
