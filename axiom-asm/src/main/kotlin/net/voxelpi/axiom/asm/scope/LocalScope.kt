package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import java.util.UUID

public sealed class LocalScope(
    public val parent: Scope,
) : Scope {

    public lateinit var startAnchor: ScopeAnchor.ScopeStart
        private set

    public lateinit var endAnchor: ScopeAnchor.ScopeEnd
        private set

    internal fun registerStartAnchor(anchor: ScopeAnchor.ScopeStart) {
        startAnchor = anchor
    }

    internal fun registerEndAnchor(anchor: ScopeAnchor.ScopeEnd) {
        endAnchor = anchor
    }

    public class Named(
        parent: Scope,
        override val uniqueId: UUID,
        public val name: String,
    ) : LocalScope(parent)

    public class Unnamed(
        parent: Scope,
        override val uniqueId: UUID,
    ) : LocalScope(parent)
}
