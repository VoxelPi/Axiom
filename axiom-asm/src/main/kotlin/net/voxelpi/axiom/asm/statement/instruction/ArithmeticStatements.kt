package net.voxelpi.axiom.asm.statement.instruction

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.argument.Argument
import net.voxelpi.axiom.instruction.Condition

public object ArithmeticStatements {

    public class AdditionStatement(
        override val source: SourceLink,
        override val input1: Argument.ValueLike,
        override val input2: Argument.ValueLike,
        override val output: Argument.RegisterLike,
        override val condition: Condition,
        override val conditionRegister: Argument.RegisterLike,
    ) : InstructionStatement.Instruction2Input1Output
}
