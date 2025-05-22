package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.type.ScopeLike

public sealed interface ScopeStatement : Statement {

    public sealed interface OpenScope : ScopeStatement {
        public data class Named(
            override val source: SourceLink,
            val name: StatementArgument<ScopeLike.ScopeName>,
        ) : OpenScope

        public data class Unnamed(
            override val source: SourceLink,
        ) : OpenScope
    }

    public data class CloseScope(
        override val source: SourceLink,
    ) : ScopeStatement
}
