package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.argument.Argument

public sealed interface ScopeStatement : Statement {

    public sealed interface OpenScope : ScopeStatement {
        public data class Named(
            override val source: SourceLink,
            val name: Argument.ScopeLike.NamedScopeReference,
        ) : OpenScope

        public data class Unnamed(
            override val source: SourceLink,
        ) : OpenScope
    }

    public data class CloseScope(
        override val source: SourceLink,
    ) : ScopeStatement
}
