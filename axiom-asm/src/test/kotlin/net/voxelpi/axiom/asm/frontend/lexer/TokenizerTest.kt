package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.source.SourceUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.assertEquals

class TokenizerTest {

    @Test
    fun `test tokenize`() {
        val source = SourceUnit(
            "test",
            """
                Hello, World!
            """.trimIndent()
        )
        val tokens = Tokenizer.tokenize(source)

        assertEquals(5, tokens.size, "Invalid number of tokens")

        assertInstanceOf<Token.Symbol>(tokens[0])
        assertEquals("Hello", (tokens[0] as? Token.Symbol)?.symbol)

        assertInstanceOf<Token.Symbol>(tokens[1])
        assertEquals(",", (tokens[1] as? Token.Symbol)?.symbol)

        assertInstanceOf<Token.Separator.Weak>(tokens[2])

        assertInstanceOf<Token.Symbol>(tokens[3])
        assertEquals("World", (tokens[3] as? Token.Symbol)?.symbol)

        assertInstanceOf<Token.Symbol>(tokens[4])
        assertEquals("!", (tokens[4] as? Token.Symbol)?.symbol)
    }

    @Test
    fun `test tokenizer comments`() {
        val source = SourceUnit(
            "test",
            """
                Hello # World!
            """.trimIndent()
        )
        val tokens = Tokenizer.tokenize(source)

        assertEquals(1, tokens.size, "Invalid number of tokens")
    }
}
