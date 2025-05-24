package net.voxelpi.axiom.asm.anchor

import java.util.UUID

public sealed interface ScopeAnchor : Anchor {

    public val scope: UUID

    public data class ScopeStart(
        override val uniqueId: UUID,
        override val scope: UUID,
    ) : ScopeAnchor

    public data class ScopeEnd(
        override val uniqueId: UUID,
        override val scope: UUID,
    ) : ScopeAnchor
}
