package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NamespacedIdTransformationTest {

    @Test
    fun `test simple namespace`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                this::is::a::namespace
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(10, tokens.size)

        tokens = NamespacedIdTransformation.transform(tokens)
        assertEquals(1, tokens.size)
        assertIs<LexerToken.Text>(tokens[0])
        assertEquals((tokens[0] as LexerToken.Text).value, "this::is::a::namespace")
    }
}
