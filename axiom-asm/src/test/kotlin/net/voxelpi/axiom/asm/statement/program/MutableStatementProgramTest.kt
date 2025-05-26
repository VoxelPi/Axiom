package net.voxelpi.axiom.asm.statement.program

import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MutableStatementProgramTest {

    @Test
    fun `test scope sorting`() {
        val globalScope = GlobalScope()
        val program = MutableStatementProgram(globalScope, emptyList())

        val scope1 = globalScope.createScope("scope1")
        val scope11 = scope1.createScope("scope11")
        val scope12 = scope1.createScope("scope12")
        val scope121 = scope12.createScope("scope121")
        val scope2 = globalScope.createScope("scope2")
        val scope21 = scope2.createScope("scope21")
        val scope22 = scope2.createScope("scope22")
        val scope23 = scope2.createScope("scope23")

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
        val program = MutableStatementProgram(globalScope, emptyList())

        // Register 100 scopes with random parent scopes.
        repeat(1000) { index ->
            val parentScope = program.scopes.values.random()
            val scope = parentScope.createScope("scope_$index")
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
