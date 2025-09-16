package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.source.SourceUnit
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LexerTest {
    @Test
    fun `test lexer`() {
        val source = SourceUnit(
            "test",
            """
                !module test.test2 {
                    var a = 4
                    a += 3
                }
            """.trimIndent()
        )

        val tokens = Lexer.tokenize(source)

        assertEquals(8, tokens.size)
    }
}
