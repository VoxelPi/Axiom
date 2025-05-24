package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.statement.StatementSet
import net.voxelpi.axiom.asm.type.ScopeLike

public object ScopeStatement {

    public object Parameter {
        public val NAME: StatementParameter<ScopeLike.ScopeName> = StatementParameter.create("name")
    }

    public val Clopen: StatementSet = StatementSet.create("scope")

    public val Open: StatementSet = StatementSet.create("scope/open", Clopen)

    public val OpenNamed: Statement = Statement.create("scope/open/named", Open) {
        declare(Parameter.NAME)
    }

    public val OpenUnnamed: Statement = Statement.create("scope/open/unnamed", Open)

    public val Close: Statement = Statement.create("include_statement/unit", Clopen)
}
