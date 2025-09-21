package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.source.SourceUnit
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterTokenTransformationTest {

    @Test
    fun `test simple character`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                'a'
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(3, tokens.size)

        tokens = CharacterTokenTransformation.transform(tokens)
        assertEquals(1, tokens.size)
    }

    @Test
    fun `test escaped character`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                '\''
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(4, tokens.size)

        tokens = CharacterTokenTransformation.transform(tokens)
        assertEquals(1, tokens.size)
    }

    @Test
    fun `test foreign character`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                'ä'
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(3, tokens.size)

        tokens = CharacterTokenTransformation.transform(tokens)
        assertEquals(1, tokens.size)
        assertEquals("ä".codePointAt(0).toLong(), (tokens[0] as? LexerToken.Integer)?.value)
    }

    @Test
    fun `test emoji character`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                '💀'
            """.trimIndent()
        )
        var tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(3, tokens.size)

        tokens = CharacterTokenTransformation.transform(tokens)
        assertEquals(1, tokens.size)
        assertEquals("💀".codePointAt(0).toLong(), (tokens[0] as? LexerToken.Integer)?.value)
    }

    @Test
    fun `test invalid escaped character`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                '\a'
            """.trimIndent()
        )
        val tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(4, tokens.size)

        assertThrows<SourcedCompilationException> { CharacterTokenTransformation.transform(tokens) }
    }

    @Test
    fun `test invalid character`() {
        val sourceUnit = SourceUnit(
            "test",
            """
                'abc'
            """.trimIndent()
        )
        val tokens = Tokenizer.tokenize(sourceUnit)
        assertEquals(3, tokens.size)

        assertThrows<SourcedCompilationException> { CharacterTokenTransformation.transform(tokens) }
    }
}
