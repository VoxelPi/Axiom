package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LabelTokenTransformationTest {

    @Test
    fun `test simple label`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                @my_label
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        tokens = NamespacedIdTransformation.transform(tokens)
        tokens = PlaceholderTokenTransformation.transform(tokens)
        tokens = LabelTokenTransformation.transform(tokens)

        assertEquals(1, tokens.size)
        assertIs<LexerToken.Label>(tokens[0])
        assertEquals((tokens[0] as LexerToken.Label).id.toString(), "my_label")
    }

    @Test
    fun `test namespaced label`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                @my_library::my_module::my_label
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        tokens = NamespacedIdTransformation.transform(tokens)
        tokens = PlaceholderTokenTransformation.transform(tokens)
        tokens = LabelTokenTransformation.transform(tokens)

        assertEquals(1, tokens.size)
        assertIs<LexerToken.Label>(tokens[0])
        assertEquals((tokens[0] as LexerToken.Label).id.toString(), "my_library::my_module::my_label")
    }
}
