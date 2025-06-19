package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.ScopeLike

public sealed interface ScopeStatement {

    public sealed interface Open : ScopeStatement {

        @StatementType("scope/open/named")
        public data class Named(
            public val name: ScopeLike.ScopeName,
        ) : Open

        @StatementType("scope/open/unnamed")
        public data object Unnamed : Open

        @StatementType("scope/open/named_at")
        public data class NamedAt(
            public val name: ScopeLike.ScopeName,
            public val position: IntegerValue,
        ) : Open

        @StatementType("scope/open/unnamed_at")
        public data class UnnamedAt(
            public val position: IntegerValue,
        ) : Open
    }

    @StatementType("scope/close")
    public data object Close : ScopeStatement
}
