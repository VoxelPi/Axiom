package net.voxelpi.axiom.asm.statement.sequence

import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MutableStatementSequenceTest {

    @Test
    fun `test scope sorting`() {
        val globalScope = GlobalScope()
        val program = MutableStatementSequence(globalScope, emptyList())

        val scope1 = LocalScope.Named(globalScope, UUID.randomUUID(), "scope1", emptyMap(), emptyMap())
        val scope11 = LocalScope.Named(scope1, UUID.randomUUID(), "scope11", emptyMap(), emptyMap())
        val scope12 = LocalScope.Named(scope1, UUID.randomUUID(), "scope12", emptyMap(), emptyMap())
        val scope121 = LocalScope.Named(scope12, UUID.randomUUID(), "scope121", emptyMap(), emptyMap())
        val scope2 = LocalScope.Named(globalScope, UUID.randomUUID(), "scope2", emptyMap(), emptyMap())
        val scope21 = LocalScope.Named(scope2, UUID.randomUUID(), "scope21", emptyMap(), emptyMap())
        val scope22 = LocalScope.Named(scope2, UUID.randomUUID(), "scope22", emptyMap(), emptyMap())
        val scope23 = LocalScope.Named(scope2, UUID.randomUUID(), "scope23", emptyMap(), emptyMap())

        fun registerScope(scope: LocalScope) {
            program.scopes[scope.uniqueId] = scope
        }

        registerScope(scope22)
        registerScope(scope11)
        registerScope(scope2)
        registerScope(scope121)
        registerScope(scope23)
        registerScope(scope21)
        registerScope(scope1)
        registerScope(scope12)

        // Sort scopes.
        val sortedScopes = program.sortedScopes()

        // Check that scopes are sorted.
        for (iScope in sortedScopes.indices) {
            val scope = sortedScopes[iScope]
            when (scope) {
                is GlobalScope -> {
                    assertEquals(0, iScope)
                }
                is LocalScope -> {
                    assertContains(sortedScopes.take(iScope), scope.parent)
                }
            }
        }
    }

    @Test
    fun `test randomized scope sorting`() {
        val globalScope = GlobalScope()
        val program = MutableStatementSequence(globalScope, emptyList())

        // Register 100 scopes with random parent scopes.
        repeat(1000) { index ->
            val parentScope = program.scopes.values.random()
            val scope = LocalScope.Named(parentScope, UUID.randomUUID(), "scope_$index", emptyMap(), emptyMap())
            program.scopes[scope.uniqueId] = scope
        }

        // Sort scopes.
        val sortedScopes = program.sortedScopes()

        // Check that scopes are sorted.
        for (iScope in sortedScopes.indices) {
            val scope = sortedScopes[iScope]
            when (scope) {
                is GlobalScope -> {
                    assertEquals(0, iScope)
                }
                is LocalScope -> {
                    assertContains(sortedScopes.take(iScope), scope.parent)
                }
            }
        }
    }
}
