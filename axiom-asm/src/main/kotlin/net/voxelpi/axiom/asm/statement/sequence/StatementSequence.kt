package net.voxelpi.axiom.asm.statement.sequence

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import java.util.UUID

public interface StatementSequence {

    public val globalScope: GlobalScope

    public val statements: List<StatementInstance<*>>

    public val scopes: Map<UUID, Scope>

    public val anchors: Map<UUID, Anchor>

    public fun sortedScopes(): List<Scope> {
        val scopeChildCounter = scopes.keys.associateWith { 0 }.toMutableMap()
        for (scope in scopes.values) {
            if (scope is LocalScope) {
                scopeChildCounter[scope.parent.uniqueId] = scopeChildCounter[scope.parent.uniqueId]!! + 1
            }
        }

        val sortedScopes = ArrayDeque<Scope>()

        val leafNodes = ArrayDeque<UUID>()
        leafNodes.addAll(scopeChildCounter.filter { it.value == 0 }.keys) // Collect all leaf nodes that are nodes with no children.
        scopeChildCounter.keys.removeAll(leafNodes) // Remove all leaves nodes as they have no children.
        while (leafNodes.isNotEmpty()) {
            val leafNodeId = leafNodes.removeFirst()
            val leafNode = scopes[leafNodeId]!!
            sortedScopes.addFirst(leafNode)

            when (leafNode) {
                is LocalScope -> {
                    scopeChildCounter[leafNode.parent.uniqueId] = scopeChildCounter[leafNode.parent.uniqueId]!! - 1 // Reduce parent child counter by one.
                    if (scopeChildCounter[leafNode.parent.uniqueId] == 0) {
                        leafNodes.addLast(leafNode.parent.uniqueId) // Add to leaf nodes.
                        scopeChildCounter.remove(leafNode.parent.uniqueId) // Remove from the child-count map.
                    }
                }
                is GlobalScope -> {
                    check(leafNodes.isEmpty()) { "Scope graph is not connected." }
                }
            }
        }

        return sortedScopes.toList()
    }
}
