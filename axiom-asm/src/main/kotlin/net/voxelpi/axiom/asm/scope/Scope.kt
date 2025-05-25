package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public sealed interface Scope {

    public val uniqueId: UUID

    public val scopes: MutableList<Scope>

    public val variables: MutableMap<String, Variable>

    public val labels: MutableMap<String, Label>

    public fun createScope(
        uniqueId: UUID = UUID.randomUUID(),
        variables: MutableMap<String, Variable> = mutableMapOf(),
        labels: MutableMap<String, Label> = mutableMapOf(),
        scopeStartAnchorUniqueId: UUID = UUID.randomUUID(),
        scopeEndAnchorUniqueId: UUID = UUID.randomUUID(),
    ): LocalScope.Unnamed {
        val scope = LocalScope.Unnamed(this, mutableListOf(), uniqueId, variables, labels, scopeStartAnchorUniqueId, scopeEndAnchorUniqueId)
        scopes.add(scope)
        return scope
    }

    public fun createScope(
        name: String,
        uniqueId: UUID = UUID.randomUUID(),
        variables: MutableMap<String, Variable> = mutableMapOf(),
        labels: MutableMap<String, Label> = mutableMapOf(),
        scopeStartAnchorUniqueId: UUID = UUID.randomUUID(),
        scopeEndAnchorUniqueId: UUID = UUID.randomUUID(),
    ): LocalScope.Named {
        val scope = LocalScope.Named(this, mutableListOf(), uniqueId, name, variables, labels, scopeStartAnchorUniqueId, scopeEndAnchorUniqueId)
        scopes.add(scope)
        return scope
    }

    public fun defineVariable(name: String, value: ValueLike): Variable {
        val variable = Variable(name, value)
        this.variables[name] = variable
        return variable
    }

    public fun updateVariable(name: String, value: ValueLike): Variable {
        val variable = Variable(name, value)
        this.variables[name] = variable
        return variable
    }

    public fun defineLabel(name: String): Label {
        val label = Label(UUID.randomUUID(), name)
        this.labels[name] = label
        return label
    }
}
