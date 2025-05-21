package net.voxelpi.axiom.asm.statement.instruction

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.instruction.Condition

public interface InstructionStatement : Statement {

    public val condition: Condition
    public val conditionRegister: String

    public interface Instruction0Input0Output : InstructionStatement

    public interface Instruction0Input1Output : InstructionStatement {
        public val output: String
    }

    public interface Instruction1Input0Output : InstructionStatement {
        public val input: String
    }

    public interface Instruction1Input1Output : InstructionStatement {
        public val input: String
        public val output: String
    }

    public interface Instruction2Input0Output : InstructionStatement {
        public val input1: String
        public val input2: String
    }

    public interface Instruction2Input1Output : InstructionStatement {
        public val input1: String
        public val input2: String
        public val output: String
    }
}
