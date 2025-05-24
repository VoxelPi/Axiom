package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.statement.StatementSet
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike

public object IncludeStatement {

    public object Parameter {
        public val UNIT: StatementParameter<UnitLike> = StatementParameter.create("unit")
        public val SCOPE: StatementParameter<ScopeLike.ScopeName> = StatementParameter.create("scope")
        public val ALIAS: StatementParameter<ScopeLike.ScopeName> = StatementParameter.create("alias")
    }

    public val Include: StatementSet = StatementSet.create("include_statement") {
        declare(Parameter.UNIT)
    }

    public val ScopedInclude: StatementSet = StatementSet.create("include_statement/scoped", Include) {
        declare(Parameter.SCOPE)
    }

    public val IncludeUnit: Statement = Statement.create("include_statement/unit", Include)

    public val IncludeScopeFromUnit: Statement = Statement.create("include_statement/unit", ScopedInclude)

    public val IncludeScopeFromUnitAsAlias: Statement = Statement.create("include_statement/unit", ScopedInclude) {
        declare(Parameter.ALIAS)
    }
}
