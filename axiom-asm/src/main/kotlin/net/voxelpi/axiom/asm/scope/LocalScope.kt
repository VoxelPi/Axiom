package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.variable.Variable
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
        override val variables: Map<String, Variable>,
        override val labels: Map<String, Label>,
    ) : LocalScope(parent)

    public class Unnamed(
        parent: Scope,
        override val uniqueId: UUID,
        override val variables: Map<String, Variable>,
        override val labels: Map<String, Label>,
    ) : LocalScope(parent)
}
