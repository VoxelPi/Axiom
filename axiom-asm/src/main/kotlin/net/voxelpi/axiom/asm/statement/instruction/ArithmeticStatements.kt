package net.voxelpi.axiom.asm.statement.instruction

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.instruction.Condition

public object ArithmeticStatements {

    public class AdditionStatement(
        override val source: SourceLink,
        override val input1: String,
        override val input2: String,
        override val output: String,
        override val condition: Condition,
        override val conditionRegister: String,
    ) : InstructionStatement.Instruction2Input1Output
}
