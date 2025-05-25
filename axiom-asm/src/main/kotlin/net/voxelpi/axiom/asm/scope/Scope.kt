package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.exception.CompilationException
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

    public fun defineVariable(name: String, value: ValueLike): Result<Variable> {
        if (isVariableDefined(name)) {
            return Result.failure(CompilationException("The variable $name is already defined in this scope stack."))
        }

        val variable = Variable(name, value)
        this.variables[name] = variable
        return Result.success(variable)
    }

    public fun updateVariable(name: String, value: ValueLike): Result<Variable> {
        if (name !in variables) {
            return Result.failure(CompilationException("The variable $name is not defined in this scope."))
        }
        val variable = Variable(name, value)
        this.variables[name] = variable
        return Result.success(variable)
    }

    public fun defineLabel(name: String): Result<Label> {
        if (isVariableDefined(name)) {
            return Result.failure(CompilationException("The variable $name is already defined in this scope stack."))
        }

        val label = Label(UUID.randomUUID(), name)
        this.labels[name] = label
        return Result.success(label)
    }

    public fun isLabelDefined(name: String): Boolean

    public fun isLabelDefinedByChild(name: String): Boolean {
        return scopes.any { it.labels.containsKey(name) || it.isLabelDefinedByChild(name) }
    }

    public fun isVariableDefined(name: String): Boolean

    public fun findLabel(name: String): Pair<Label, Scope>?

    public fun findLabelInChild(name: String): Pair<Label, Scope>? {
        // BFS to find the label in the child scopes.
        var scopesToCheckNext = scopes.toMutableList()
        while (scopesToCheckNext.isNotEmpty()) {
            val scopesToCheck = scopesToCheckNext
            scopesToCheckNext = mutableListOf()

            for (scope in scopesToCheck) {
                // Check scope
                scope.labels[name]?.let { return it to scope }

                // Queue children for the next round.
                scopesToCheckNext.addAll(scope.scopes)
            }
        }

        return null
    }

    public fun findVariable(name: String): Pair<Variable, Scope>?
}
