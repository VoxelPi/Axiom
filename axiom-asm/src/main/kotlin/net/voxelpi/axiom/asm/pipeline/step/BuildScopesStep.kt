package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.ScopeStatement
import java.util.UUID
import kotlin.collections.set

public object BuildScopesStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Update scope of all variables.
        val scopeStack = ArrayDeque<Scope>(listOf(program.globalScope))
        return program.transform { statementInstance ->
            val statement = statementInstance.create()

            when (statement) {
                is ScopeStatement.Open.Named, is ScopeStatement.Open.NamedAt -> {
                    val (scopeName, scopePosition) = when (statement) {
                        is ScopeStatement.Open.NamedAt -> Pair(statement.name.name, statement.position.value.toInt())
                        is ScopeStatement.Open.Named -> Pair(statement.name.name, null)
                        else -> throw IllegalStateException("Unexpected scope statement $statement")
                    }

                    val parentScope = scopeStack.last()

                    // Create a label.
                    val label = Label(UUID.randomUUID(), scopeName)
                    program.anchors[label.uniqueId] = label
                    parentScope.labels[label.name] = label
                    yield(ANCHOR_PROTOTYPE.createInstance(AnchorStatement(label), parentScope, statementInstance.source))

                    // Create a new name scope and push it to the scope stack.
                    val scope = parentScope.createScope(scopeName, position = scopePosition)
                    program.scopes[scope.uniqueId] = scope
                    program.anchors[scope.startAnchor.uniqueId] = scope.startAnchor
                    program.anchors[scope.endAnchor.uniqueId] = scope.endAnchor
                    scopeStack.addLast(scope)

                    // Yield the start anchor of the scope.
                    yield(ANCHOR_PROTOTYPE.createInstance(AnchorStatement(scope.startAnchor), scope, statementInstance.source))
                }
                is ScopeStatement.Open.Unnamed, is ScopeStatement.Open.UnnamedAt -> {
                    val scopePosition = when (statement) {
                        is ScopeStatement.Open.UnnamedAt -> statement.position.value.toInt()
                        is ScopeStatement.Open.Unnamed -> null
                        else -> throw IllegalStateException("Unexpected scope statement $statement")
                    }

                    // Create a new unnamed scope and push it to the scope stack.
                    val parentScope = scopeStack.last()
                    val scope = parentScope.createScope(position = scopePosition)
                    program.scopes[scope.uniqueId] = scope
                    program.anchors[scope.startAnchor.uniqueId] = scope.startAnchor
                    program.anchors[scope.endAnchor.uniqueId] = scope.endAnchor
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

    private val ANCHOR_PROTOTYPE = StatementPrototype.fromType(AnchorStatement::class).getOrThrow()
}
