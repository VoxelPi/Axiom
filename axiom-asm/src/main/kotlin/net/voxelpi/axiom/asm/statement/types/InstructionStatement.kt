package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementSet
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public object InstructionStatement {

    public object Parameter {
        public val OPERATION: StatementParameter<Operation> = StatementParameter.create("operation")
        public val CONDITION: StatementParameter<Condition> = StatementParameter.create("condition")
        public val CONDITION_VALUE: StatementParameter<RegisterLike> = StatementParameter.create("condition_value")
        public val INPUT_A: StatementParameter<ValueLike> = StatementParameter.create("input_a")
        public val INPUT_B: StatementParameter<ValueLike> = StatementParameter.create("input_b")
        public val OUTPUT: StatementParameter<RegisterLike> = StatementParameter.create("output")
    }

    public val Instruction: StatementSet = StatementSet.create("instruction_statement") {
        declare(Parameter.OPERATION)
        declare(Parameter.CONDITION)
        declare(Parameter.CONDITION_VALUE)
        declare(Parameter.INPUT_A)
        declare(Parameter.INPUT_B)
    }

    public val InstructionWithOutput: Statement = Statement.create("instruction_statement/instruction_with_output", Instruction) {
        declare(Parameter.OUTPUT)
    }

    public val InstructionWithoutOutput: Statement = Statement.create("instruction_statement/instruction_without_output", Instruction)
}
