package net.voxelpi.axiom.asm.statement.sequence

import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.types.ScopeStatement
import java.util.UUID
import kotlin.reflect.full.isSubclassOf

public class MutableStatementSequence(
    public val globalScope: GlobalScope,
    public val statements: MutableList<StatementInstance<*>>,
    public val scopes: MutableMap<UUID, Scope> = mutableMapOf(),
    public val anchors: MutableMap<UUID, ScopeAnchor> = mutableMapOf(),
) {
    public constructor(
        globalScope: GlobalScope,
        statements: List<StatementInstance<*>>,
    ) : this(globalScope, statements.toMutableList(), mutableMapOf(), mutableMapOf())

    public fun copy(): MutableStatementSequence {
        return MutableStatementSequence(
            globalScope,
            statements.toMutableList(),
            scopes.toMutableMap(),
            anchors.toMutableMap(),
        )
    }

    public fun transform(transformation: suspend SequenceScope<StatementInstance<*>>.(statement: StatementInstance<*>) -> Unit): Result<Unit> {
        val previousStatements = statements.toList()
        statements.clear()

        try {
            val newStatements = sequence {
                for (previousStatement in previousStatements) {
                    this.transformation(previousStatement)
                }
            }.toList()
            statements.addAll(newStatements)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
        return Result.success(Unit)
    }

    public inline fun <reified T : Any> transformType(noinline transformation: suspend SequenceScope<StatementInstance<*>>.(statement: StatementInstance<out T>) -> Unit): Result<Unit> {
        return transform { statement ->
            if (statement.prototype.type.isSubclassOf(T::class)) {
                @Suppress("UNCHECKED_CAST")
                this.transformation(statement as StatementInstance<T>)
            } else {
                yield(statement)
            }
        }
    }

    public fun buildScopes() {
        // Update scope of all variables.
        val scopeStack = ArrayDeque<Scope>(listOf(globalScope))
        transform { statementInstance ->
            val statement = statementInstance.build()

            when (statement) {
                is ScopeStatement.Open.Named -> {
                    // Create a new name scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = LocalScope.Named(parentScope, UUID.randomUUID(), statement.name.name, emptyMap(), emptyMap())
                    scope.registerStartAnchor(ScopeAnchor.ScopeStart(UUID.randomUUID(), scope.uniqueId))
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(SCOPE_START_ANCHOR_PROTOTYPE.createInstance(scope.startAnchor, scope, statementInstance.source))
                }
                is ScopeStatement.Open.Unnamed -> {
                    // Create a new unnamed scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = LocalScope.Unnamed(parentScope, UUID.randomUUID(), emptyMap(), emptyMap())
                    scope.registerStartAnchor(ScopeAnchor.ScopeStart(UUID.randomUUID(), scope.uniqueId))
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(SCOPE_START_ANCHOR_PROTOTYPE.createInstance(scope.startAnchor, scope, statementInstance.source))
                }
                is ScopeStatement.Close -> {
                    // Make sure that the scope stack is not empty.
                    if (scopeStack.size <= 1) {
                        throw IllegalStateException("Cannot close the global scope.")
                    }

                    // Remove scope from the scope stack.
                    val scope = scopeStack.removeLast() as LocalScope

                    // Yield the end anchor of the scope.
                    scope.registerEndAnchor(ScopeAnchor.ScopeEnd(UUID.randomUUID(), scope.uniqueId))
                    yield(SCOPE_END_ANCHOR_PROTOTYPE.createInstance(scope.endAnchor, scope, statementInstance.source))
                }
                else -> {
                    // Update scope of statement prototype.
                    yield(
                        StatementInstance(
                            statementInstance.prototype,
                            scopeStack.last(),
                            statementInstance.source,
                            statementInstance.parameterValues,
                            statementInstance.parameterSources,
                        )
                    )
                }
            }
        }
    }

    public companion object {
        private val SCOPE_START_ANCHOR_PROTOTYPE = StatementPrototype.fromType(ScopeAnchor.ScopeStart::class).getOrThrow()
        private val SCOPE_END_ANCHOR_PROTOTYPE = StatementPrototype.fromType(ScopeAnchor.ScopeEnd::class).getOrThrow()
    }
}
