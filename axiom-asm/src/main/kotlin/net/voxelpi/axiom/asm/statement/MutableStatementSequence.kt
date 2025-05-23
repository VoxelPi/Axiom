package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import java.util.UUID
import kotlin.reflect.full.isSubclassOf

public class MutableStatementSequence(
    public val globalScope: GlobalScope,
    public val statements: MutableList<Statement>,
    public val scopes: MutableMap<UUID, Scope> = mutableMapOf(),
    public val anchors: MutableMap<UUID, ScopeAnchor> = mutableMapOf(),
) {
    public constructor(globalScope: GlobalScope, statements: List<Statement>) : this(globalScope, statements.toMutableList(), mutableMapOf(), mutableMapOf())

    public fun copy(): MutableStatementSequence {
        return MutableStatementSequence(
            globalScope,
            statements.toMutableList(),
            scopes.toMutableMap(),
            anchors.toMutableMap(),
        )
    }

    public fun transform(transformation: suspend SequenceScope<Statement>.(statement: Statement) -> Unit): Result<Unit> {
        val previousStatements = statements.toList()
        statements.clear()

        try {
            val newStatements = sequence {
                for (previousStatement in statements) {
                    this.transformation(previousStatement)
                }
            }.toList()
            statements.addAll(newStatements)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
        return Result.success(Unit)
    }

    public inline fun <reified T : Statement> transformType(noinline transformation: suspend SequenceScope<Statement>.(statement: T) -> Unit): Result<Unit> {
        return transform { statement ->
            if (statement is T) {
                this.transformation(statement)
            } else {
                yield(statement)
            }
        }
    }

    public inline fun <reified T : Statement> buildPrototypesWithType(): Result<Unit> {
        return transformType<StatementPrototype<*>> { statement ->
            if (statement.type.isSubclassOf(T::class)) {
                yield(statement.build().getOrThrow())
            } else {
                yield(statement)
            }
        }
    }

    public fun buildScopes() {
        // Build scope statements.
        buildPrototypesWithType<ScopeStatement>()

        // Update scope of all variables.
        val scopeStack = ArrayDeque<Scope>(listOf(globalScope))
        transform { statement ->
            when (statement) {
                is ScopeStatement.OpenScope.Named -> {
                    // Create a new name scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = LocalScope.Named(parentScope, UUID.randomUUID(), statement.name.value.name)
                    scope.registerStartAnchor(ScopeAnchor.ScopeStart(statement.source, UUID.randomUUID(), scope.uniqueId))
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(scope.startAnchor)
                }
                is ScopeStatement.OpenScope.Unnamed -> {
                    // Create a new unnamed scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = LocalScope.Unnamed(parentScope, UUID.randomUUID())
                    scope.registerStartAnchor(ScopeAnchor.ScopeStart(statement.source, UUID.randomUUID(), scope.uniqueId))
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(scope.startAnchor)
                }
                is ScopeStatement.CloseScope -> {
                    // Make sure that the scope stack is not empty.
                    if (scopeStack.size <= 1) {
                        throw IllegalStateException("Cannot close the global scope.")
                    }

                    // Remove scope from the scope stack.
                    val scope = scopeStack.removeLast() as LocalScope

                    // Yield the end anchor of the scope.
                    scope.registerEndAnchor(ScopeAnchor.ScopeEnd(statement.source, UUID.randomUUID(), scope.uniqueId))
                    yield(scope.endAnchor)
                }
                is StatementPrototype<*> -> {
                    // Update scope of statement prototype.
                    yield(StatementPrototype(statement.source, statement.type, scopeStack.last(), statement.arguments))
                }
                else -> {
                    // No changes to the statement.
                    yield(statement)
                }
            }
        }
    }
}
