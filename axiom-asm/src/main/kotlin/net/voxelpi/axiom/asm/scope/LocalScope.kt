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

    override fun findScope(name: String): Scope? {
        scopes.find { it is Named && it.name == name }?.let { return it }

        // Check parents downwards.
        var parent: Scope? = this
        while (parent != null) {
            if (parent is Named && parent.name == name) {
                return parent
            }
            parent = if (parent is LocalScope) parent.parent else null
        }

        return null
    }

    override fun isLabelDefined(name: String): Boolean {
        return labels.containsKey(name) || parent.isLabelDefined(name) || isLabelDefinedByChild(name)
    }

    override fun isVariableDefined(name: String): Boolean {
        return variables.containsKey(name) || parent.isVariableDefined(name)
    }

    override fun findLabel(name: String): Pair<Label, Scope>? {
        // Check the current scope.
        labels[name]?.let { return it to this }

        // Check parents downwards.
        var parent: Scope? = this
        while (parent != null) {
            parent.labels[name]?.let { return it to parent }
            parent = if (parent is LocalScope) parent.parent else null
        }

        // Check children upwards.
        return findLabelInChild(name)
    }

    override fun findVariable(name: String): Pair<Variable, Scope>? {
        // Check the current scope.
        variables[name]?.let { return it to this }

        // Check parents downwards.
        var parent: Scope? = this
        while (parent != null) {
            parent.variables[name]?.let { return it to parent }
            parent = if (parent is LocalScope) parent.parent else null
        }

        return null
    }

    override fun findVariable(uniqueId: UUID): Pair<Variable, Scope>? {
        variables.values.find { it.uniqueId == uniqueId }?.let { return it to this }

        // Check parents downwards.
        var parent: Scope? = this
        while (parent != null) {
            parent.variables.values.find { it.uniqueId == uniqueId }?.let { return it to parent }
            parent = if (parent is LocalScope) parent.parent else null
        }

        return null
    }

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
