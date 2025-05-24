package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike

public sealed interface IncludeStatement {

    public val unit: UnitLike

    @StatementType("include/unit")
    public data class Unit(
        override val unit: UnitLike,
    ) : IncludeStatement

    public sealed interface Scope : IncludeStatement {

        public val scope: ScopeLike.ScopeName

        @StatementType("include/scope")
        public data class Direct(
            override val unit: UnitLike,
            override val scope: ScopeLike.ScopeName,
        ) : Scope

        @StatementType("include/scope/with_alias")
        public data class WithAlias(
            override val unit: UnitLike,
            override val scope: ScopeLike.ScopeName,
            val alias: ScopeLike.ScopeName,
        ) : Scope
    }
}
