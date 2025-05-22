package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike

public sealed interface IncludeStatement : Statement {

    public val unit: StatementArgument<UnitLike>

    public data class Unit(
        override val source: SourceLink,
        override val unit: StatementArgument<UnitLike>,
    ) : IncludeStatement

    public interface Scope : IncludeStatement {

        public data class Direct(
            override val source: SourceLink,
            override val unit: StatementArgument<UnitLike>,
            val scope: StatementArgument<ScopeLike.ScopeName>,
        ) : Scope

        public data class WithAlias(
            override val source: SourceLink,
            override val unit: StatementArgument<UnitLike>,
            val scope: StatementArgument<ScopeLike.ScopeName>,
            val alias: StatementArgument<ScopeLike.ScopeName>,
        ) : Scope
    }
}
