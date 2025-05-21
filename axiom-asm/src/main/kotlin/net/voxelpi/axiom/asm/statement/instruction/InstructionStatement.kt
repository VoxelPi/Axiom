package net.voxelpi.axiom.asm.statement.instruction

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.argument.Argument
import net.voxelpi.axiom.instruction.Condition

public interface InstructionStatement : Statement {

    public val condition: Condition
    public val conditionRegister: Argument.RegisterLike

    public interface Instruction0Input0Output : InstructionStatement

    public interface Instruction0Input1Output : InstructionStatement {
        public val output: Argument.RegisterLike
    }

    public interface Instruction1Input0Output : InstructionStatement {
        public val input: Argument.ValueLike
    }

    public interface Instruction1Input1Output : InstructionStatement {
        public val input: Argument.ValueLike
        public val output: Argument.RegisterLike
    }

    public interface Instruction2Input0Output : InstructionStatement {
        public val input1: Argument.ValueLike
        public val input2: Argument.ValueLike
    }

    public interface Instruction2Input1Output : InstructionStatement {
        public val input1: Argument.ValueLike
        public val input2: Argument.ValueLike
        public val output: Argument.RegisterLike
    }
}
