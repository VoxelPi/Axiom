package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.statement.annotation.StatementType
import java.util.UUID

public sealed interface ScopeAnchor : Anchor {

    public val scope: UUID

    @StatementType("scope/start")
    public data class ScopeStart(
        override val uniqueId: UUID,
        override val scope: UUID,
    ) : ScopeAnchor

    @StatementType("scope/end")
    public data class ScopeEnd(
        override val uniqueId: UUID,
        override val scope: UUID,
    ) : ScopeAnchor
}
