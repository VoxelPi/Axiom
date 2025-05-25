package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public sealed class LocalScope(
    public val parent: Scope,
    scopeStartAnchorUniqueId: UUID,
    scopeEndAnchorUniqueId: UUID,
) : Scope {

    public val startAnchor: ScopeAnchor.ScopeStart = ScopeAnchor.ScopeStart(scopeStartAnchorUniqueId, this)

    public val endAnchor: ScopeAnchor.ScopeEnd = ScopeAnchor.ScopeEnd(scopeEndAnchorUniqueId, this)

    public class Named(
        parent: Scope,
        override val scopes: MutableList<Scope>,
        override val uniqueId: UUID,
        public val name: String,
        override val variables: MutableMap<String, Variable>,
        override val labels: MutableMap<String, Label>,
        scopeStartAnchorUniqueId: UUID = UUID.randomUUID(),
        scopeEndAnchorUniqueId: UUID = UUID.randomUUID(),
    ) : LocalScope(parent, scopeStartAnchorUniqueId, scopeEndAnchorUniqueId)

    public class Unnamed(
        parent: Scope,
        override val scopes: MutableList<Scope>,
        override val uniqueId: UUID,
        override val variables: MutableMap<String, Variable>,
        override val labels: MutableMap<String, Label>,
        scopeStartAnchorUniqueId: UUID = UUID.randomUUID(),
        scopeEndAnchorUniqueId: UUID = UUID.randomUUID(),
    ) : LocalScope(parent, scopeStartAnchorUniqueId, scopeEndAnchorUniqueId)
}
