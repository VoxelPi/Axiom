package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class TextTokenTransformationTest {

    @Test
    fun `test simple string`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                "Hello, World!"
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(7, tokens.size)

        tokens = TextTokenTransformation.transform(tokens)
        assertEquals(1, tokens.size)
    }

    @Test
    fun `test escaped string`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                "Hello, \" World!"
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(10, tokens.size)

        tokens = TextTokenTransformation.transform(tokens)
        assertEquals(1, tokens.size)
    }

    @Test
    fun `test multiple strings`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                "Hello, \" World!" "test"
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(14, tokens.size)

        tokens = TextTokenTransformation.transform(tokens)
        assertEquals(3, tokens.size)
    }
}
