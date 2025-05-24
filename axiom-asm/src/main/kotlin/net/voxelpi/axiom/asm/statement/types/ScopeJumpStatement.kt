package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.statement.StatementSet
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.instruction.Condition

public object ScopeJumpStatement {

    public object Parameter {
        public val CONDITION: StatementParameter<Condition> = StatementParameter.create("condition")
        public val CONDITION_VALUE: StatementParameter<RegisterLike> = StatementParameter.create("condition_value")
        public val SCOPE: StatementParameter<ScopeLike> = StatementParameter.create("scope")
    }

    public val Instruction: StatementSet = StatementSet.create("instruction_statement") {
        declare(Parameter.CONDITION)
        declare(Parameter.CONDITION_VALUE)
        declare(Parameter.SCOPE)
    }

    public val Exit: Statement = Statement.create("exit", Instruction)

    public val Repeat: Statement = Statement.create("repeat", Instruction)
}
