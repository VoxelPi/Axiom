package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike

public sealed interface IncludeStatement : Statement {

    public data class Unit(
        override val source: SourceLink,
        val unit: StatementArgument<UnitLike>,
    ) : IncludeStatement

    public data class Scope(
        override val source: SourceLink,
        val unit: StatementArgument<UnitLike>,
        val scope: StatementArgument<ScopeLike.ScopeName>,
    ) : IncludeStatement

    public data class ScopeWithAlias(
        override val source: SourceLink,
        val unitId: StatementArgument<UnitLike>,
        val scope: StatementArgument<ScopeLike.ScopeName>,
        val alias: StatementArgument<ScopeLike.ScopeName>,
    ) : IncludeStatement
}
