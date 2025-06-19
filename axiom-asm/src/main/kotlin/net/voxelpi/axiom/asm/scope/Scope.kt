package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public sealed interface Scope {

    public val uniqueId: UUID

    public val scopes: MutableList<Scope>

    public val variables: MutableMap<String, Variable>

    public val labels: MutableMap<String, Anchor.Named>

    public val position: Int?

    public fun createScope(
        uniqueId: UUID = UUID.randomUUID(),
        variables: MutableMap<String, Variable> = mutableMapOf(),
        labels: MutableMap<String, Anchor.Named> = mutableMapOf(),
        position: Int? = null,
        scopeStartAnchorUniqueId: UUID = UUID.randomUUID(),
        scopeEndAnchorUniqueId: UUID = UUID.randomUUID(),
    ): LocalScope.Unnamed {
        val scope = LocalScope.Unnamed(this, mutableListOf(), uniqueId, variables, labels, position, scopeStartAnchorUniqueId, scopeEndAnchorUniqueId)
        scopes.add(scope)
        return scope
    }

    public fun createScope(
        name: String,
        uniqueId: UUID = UUID.randomUUID(),
        variables: MutableMap<String, Variable> = mutableMapOf(),
        labels: MutableMap<String, Anchor.Named> = mutableMapOf(),
        position: Int? = null,
        scopeStartAnchorUniqueId: UUID = UUID.randomUUID(),
        scopeEndAnchorUniqueId: UUID = UUID.randomUUID(),
    ): LocalScope.Named {
        val scope = LocalScope.Named(this, mutableListOf(), uniqueId, name, variables, labels, position, scopeStartAnchorUniqueId, scopeEndAnchorUniqueId)
        scopes.add(scope)
        return scope
    }

    public fun findScope(name: String): Scope?

    public fun defineVariable(name: String, value: ValueLike): Result<Variable> {
        // Check if the variable is already defined in this scope.
        if (name in variables) {
            return Result.failure(CompilationException("The variable $name is already defined in this scope."))
        }

        val variable = Variable(UUID.randomUUID(), name, value)
        this.variables[name] = variable
        return Result.success(variable)
    }

    public fun updateVariable(name: String, value: ValueLike): Result<Variable> {
        val existingVariable = variables[name]
            ?: return Result.failure(CompilationException("The variable $name is not defined in this scope."))
        val variable = Variable(existingVariable.uniqueId, existingVariable.name, value)
        this.variables[name] = variable
        return Result.success(variable)
    }

    public fun defineLabel(name: String): Result<Label> {
        if (isLabelDefined(name)) {
            return Result.failure(CompilationException("The label $name is already defined in this scope stack."))
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

    public fun findLabel(name: String): Pair<Anchor.Named, Scope>?

    public fun findLabelInChild(name: String): Pair<Anchor.Named, Scope>? {
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

    public fun findVariable(uniqueId: UUID): Pair<Variable, Scope>?

    public fun ancestry(): List<Scope>

    public companion object {

        public fun lastCommonScope(scopeA: Scope, scopeB: Scope): Scope? {
            val scopeAAncestry = scopeA.ancestry()
            val scopeBAncestry = scopeB.ancestry()

            check(scopeAAncestry.isNotEmpty() && scopeBAncestry.isNotEmpty())
            return (scopeAAncestry zip scopeBAncestry).lastOrNull { it.first.uniqueId == it.second.uniqueId }?.first
        }
    }
}
