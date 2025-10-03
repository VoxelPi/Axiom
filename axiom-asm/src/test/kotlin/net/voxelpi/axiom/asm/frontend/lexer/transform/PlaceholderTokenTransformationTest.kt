package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaceholderTokenTransformationTest {

    @Test
    fun `test simple placeholder`() {
        val sourceUnit = SourceUnit(
            "test",
            $$"""
                $my_placeholder
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        tokens = NamespacedIdTransformation.transform(tokens)
        tokens = PlaceholderTokenTransformation.transform(tokens)

        assertEquals(1, tokens.size)
        assertIs<LexerToken.Placeholder>(tokens[0])
        assertEquals((tokens[0] as LexerToken.Placeholder).id.toString(), "my_placeholder")
    }

    @Test
    fun `test namespaced placeholder`() {
        val sourceUnit = SourceUnit(
            "test",
            $$"""
                $my_library::my_module::my_placeholder
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        tokens = NamespacedIdTransformation.transform(tokens)
        tokens = PlaceholderTokenTransformation.transform(tokens)

        assertEquals(1, tokens.size)
        assertIs<LexerToken.Placeholder>(tokens[0])
        assertEquals((tokens[0] as LexerToken.Placeholder).id.toString(), "my_library::my_module::my_placeholder")
    }
}
