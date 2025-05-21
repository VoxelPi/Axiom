package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.argument.Argument

public sealed interface IncludeStatement : Statement {

    public data class Unit(
        override val source: SourceLink,
        val unit: Argument.UnitLike,
    ) : IncludeStatement

    public data class Scope(
        override val source: SourceLink,
        val unit: Argument.UnitLike,
        val scope: Argument.ScopeLike.NamedScopeReference,
    ) : IncludeStatement

    public data class ScopeWithAlias(
        override val source: SourceLink,
        val unitId: Argument.UnitLike,
        val scope: Argument.ScopeLike.NamedScopeReference,
        val alias: Argument.ScopeLike.NamedScopeReference,
    ) : IncludeStatement
}
