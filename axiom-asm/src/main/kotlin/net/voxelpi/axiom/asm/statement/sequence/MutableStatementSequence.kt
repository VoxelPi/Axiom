package net.voxelpi.axiom.asm.statement.sequence

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.ScopeStatement
import java.util.UUID
import kotlin.reflect.full.isSubclassOf

public class MutableStatementSequence(
    override val globalScope: GlobalScope,
    override val statements: MutableList<StatementInstance<*>>,
    override val scopes: MutableMap<UUID, Scope> = mutableMapOf(),
    override val anchors: MutableMap<UUID, Anchor> = mutableMapOf(),
) : StatementSequence {

    public constructor(
        globalScope: GlobalScope,
        statements: List<StatementInstance<*>>,
    ) : this(globalScope, statements.toMutableList(), mutableMapOf(), mutableMapOf())

    init {
        scopes[globalScope.uniqueId] = globalScope
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
            val statement = statementInstance.create()

            when (statement) {
                is ScopeStatement.Open.Named -> {
                    // Create a new name scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = parentScope.createScope(statement.name.name)
                    scopes[scope.uniqueId] = scope
                    anchors[scope.startAnchor.uniqueId] = scope.startAnchor
                    anchors[scope.endAnchor.uniqueId] = scope.endAnchor
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(ANCHOR_PROTOTYPE.createInstance(AnchorStatement(scope.startAnchor), scope, statementInstance.source))
                }
                is ScopeStatement.Open.Unnamed -> {
                    // Create a new unnamed scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = parentScope.createScope()
                    scopes[scope.uniqueId] = scope
                    anchors[scope.startAnchor.uniqueId] = scope.startAnchor
                    anchors[scope.endAnchor.uniqueId] = scope.endAnchor
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(ANCHOR_PROTOTYPE.createInstance(AnchorStatement(scope.startAnchor), scope, statementInstance.source))
                }
                is ScopeStatement.Close -> {
                    // Make sure that the scope stack is not empty.
                    if (scopeStack.size <= 1) {
                        throw IllegalStateException("Cannot close the global scope.")
                    }

                    // Remove scope from the scope stack.
                    val scope = scopeStack.removeLast() as LocalScope

                    // Yield the end anchor of the scope.
                    yield(ANCHOR_PROTOTYPE.createInstance(AnchorStatement(scope.endAnchor), scope, statementInstance.source))
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
        private val ANCHOR_PROTOTYPE = StatementPrototype.fromType(AnchorStatement::class).getOrThrow()
    }
}
