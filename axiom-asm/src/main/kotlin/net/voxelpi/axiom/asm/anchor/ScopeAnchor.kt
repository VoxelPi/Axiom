package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.scope.LocalScope
import java.util.UUID

public sealed interface ScopeAnchor : Anchor {

    public val scope: LocalScope

    public data class ScopeStart(
        override val uniqueId: UUID,
        override val scope: LocalScope,
    ) : ScopeAnchor

    public data class ScopeEnd(
        override val uniqueId: UUID,
        override val scope: LocalScope,
    ) : ScopeAnchor
}
