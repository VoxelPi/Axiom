package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.ScopeLike

public sealed interface ScopeStatement {

    public sealed interface Open : ScopeStatement {

        @StatementType("scope/open/named")
        public data class Named(
            public val name: ScopeLike.ScopeName,
        ) : Open

        @StatementType("scope/open/unnamed")
        public data object Unnamed : Open
    }

    @StatementType("scope/close")
    public data object Close : ScopeStatement
}
