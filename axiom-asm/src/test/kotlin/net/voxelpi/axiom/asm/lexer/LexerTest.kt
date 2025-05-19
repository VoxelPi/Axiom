package net.voxelpi.axiom.asm.lexer

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.source.SourceLink
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LexerTest {

    @Test
    fun `test generated tokens`() {
        val lexer = Lexer()
        val unit = CompilationUnit("test", "this is +1 @label\nanother \$var;!include stuff")

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(3, tokenStatements.size, "Invalid number of statements")

        // Statement 1.
        val statement1 = tokenStatements[0]
        assertEquals(4, statement1.size, "Invalid number of tokens in statement 1")

        assertIs<Token.Text>(statement1[0])
        assertEquals("this", (statement1[0] as Token.Text).value)

        assertIs<Token.Text>(statement1[1])
        assertEquals("is", (statement1[1] as Token.Text).value)

        assertIs<Token.Integer>(statement1[2])
        assertEquals(1, (statement1[2] as Token.Integer).value)

        assertIs<Token.Label>(statement1[3])
        assertEquals("label", (statement1[3] as Token.Label).value)

        // Statement 2.
        val statement2 = tokenStatements[1]
        assertEquals(2, statement2.size, "Invalid number of tokens in statement 2")

        assertIs<Token.Text>(statement2[0])
        assertEquals("another", (statement2[0] as Token.Text).value)

        assertIs<Token.Variable>(statement2[1])
        assertEquals("var", (statement2[1] as Token.Variable).value)

        // Statement 3.
        val statement3 = tokenStatements[2]
        assertEquals(2, statement3.size, "Invalid number of tokens in statement 3")

        assertIs<Token.PreProcessorDirective>(statement3[0])
        assertEquals("include", (statement3[0] as Token.PreProcessorDirective).value)

        assertIs<Token.Text>(statement3[1])
        assertEquals("stuff", (statement3[1] as Token.Text).value)
    }

    @Test
    fun `test generated source references`() {
        val lexer = Lexer()
        val unit = CompilationUnit("test", "this is +1 @label\nanother \$variable;!include stuff")

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(3, tokenStatements.size, "Invalid number of statements")

        val token = tokenStatements[2][0]
        val source = SourceLink.CompilationUnitSlice(unit, 36, 1, 18, 8)

        assertEquals(source, token.source)
    }
}
