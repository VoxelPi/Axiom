package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.source.SourceLink
import java.util.UUID

public sealed interface ScopeAnchor : Anchor {

    public val scope: UUID

    public data class ScopeStart(
        override val source: SourceLink,
        override val uniqueId: UUID,
        override val scope: UUID,
    ) : ScopeAnchor

    public data class ScopeEnd(
        override val source: SourceLink,
        override val uniqueId: UUID,
        override val scope: UUID,
    ) : ScopeAnchor
}
