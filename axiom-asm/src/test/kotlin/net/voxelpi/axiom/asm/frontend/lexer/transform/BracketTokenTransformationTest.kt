package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.lexer.Tokenizer
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.source.SourceUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class BracketTokenTransformationTest {
    @Test
    fun `simple bracket example`() {
        val source = SourceUnit(
            "test",
            """
                namespace test:test2 {
                    var a = 4
                    a += 3
                }
            """.trimIndent()
        )
        val flatTokens = Tokenizer.tokenize(source)

        val tokens = BracketTokenTransformation.transform(flatTokens)

        assertEquals(7, tokens.size)
    }

    @Test
    fun `nested brackets`() {
        val source = SourceUnit(
            "test",
            """
                a {
                    b {
                        c
                    }
                }
            """.trimIndent()
        )
        val flatTokens = Tokenizer.tokenize(source)
        val tokens = TrimScopeTokenTransformation.transform(
            BracketTokenTransformation.transform(flatTokens)
        )

        assertEquals(3, tokens.size)
        assertInstanceOf<LexerToken.Text>(tokens[0])
        assertEquals("a", (tokens[0] as LexerToken.Text).value)
        assertInstanceOf<LexerToken.Separator.Weak>(tokens[1])
        assertInstanceOf<LexerToken.Bracket>(tokens[2])
        assertEquals(BracketType.CURLY, (tokens[2] as LexerToken.Bracket).type)

        val firstBracketToken = (tokens[2] as LexerToken.Bracket).tokens
        assertEquals(3, firstBracketToken.size)
        assertInstanceOf<LexerToken.Text>(firstBracketToken[0])
        assertEquals("b", (firstBracketToken[0] as LexerToken.Text).value)
        assertInstanceOf<LexerToken.Separator.Weak>(firstBracketToken[1])
        assertInstanceOf<LexerToken.Bracket>(firstBracketToken[2])
        assertEquals(BracketType.CURLY, (firstBracketToken[2] as LexerToken.Bracket).type)

        val secondBracketToken = (firstBracketToken[2] as LexerToken.Bracket).tokens
        assertEquals(1, secondBracketToken.size)
        assertInstanceOf<LexerToken.Text>(secondBracketToken[0])
        assertEquals("c", (secondBracketToken[0] as LexerToken.Text).value)
    }

    @Test
    fun `exception on missing closing bracket`() {
        val source = SourceUnit(
            "test",
            """
                {
            """.trimIndent()
        )
        val flatTokens = Tokenizer.tokenize(source)

        assertThrows<SourcedCompilationException> { BracketTokenTransformation.transform(flatTokens) }
    }

    @Test
    fun `exception on missing opening bracket`() {
        val source = SourceUnit(
            "test",
            """
                {
            """.trimIndent()
        )
        val flatTokens = Tokenizer.tokenize(source)

        assertThrows<SourcedCompilationException> { BracketTokenTransformation.transform(flatTokens) }
    }

    @Test
    fun `exception on mismatched brackets`() {
        val source = SourceUnit(
            "test",
            """
                {[}]
            """.trimIndent()
        )
        val flatTokens = Tokenizer.tokenize(source)

        assertThrows<SourcedCompilationException> { BracketTokenTransformation.transform(flatTokens) }
    }
}
