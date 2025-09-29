package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DirectiveTokenTransformationTest {

    @Test
    fun `test simple directive`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                !my_directive
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        tokens = DirectiveTokenTransformation.transform(tokens)

        assertEquals(1, tokens.size)
        assertIs<LexerToken.Directive>(tokens[0])
        assertEquals((tokens[0] as LexerToken.Directive).value, "my_directive")
    }
}
