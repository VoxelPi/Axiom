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
        assertEquals(4, statement1.tokens.size, "Invalid number of tokens in statement 1")

        assertIs<Token.Text>(statement1.tokens[0])
        assertEquals("this", (statement1.tokens[0] as Token.Text).value)

        assertIs<Token.Text>(statement1.tokens[1])
        assertEquals("is", (statement1.tokens[1] as Token.Text).value)

        assertIs<Token.Integer>(statement1.tokens[2])
        assertEquals(1, (statement1.tokens[2] as Token.Integer).value)

        assertIs<Token.Label>(statement1.tokens[3])
        assertEquals("label", (statement1.tokens[3] as Token.Label).value)

        // Statement 2.
        val statement2 = tokenStatements[1]
        assertEquals(2, statement2.tokens.size, "Invalid number of tokens in statement 2")

        assertIs<Token.Text>(statement2.tokens[0])
        assertEquals("another", (statement2.tokens[0] as Token.Text).value)

        assertIs<Token.Variable>(statement2.tokens[1])
        assertEquals("var", (statement2.tokens[1] as Token.Variable).value)

        // Statement 3.
        val statement3 = tokenStatements[2]
        assertEquals(3, statement3.tokens.size, "Invalid number of tokens in statement 3")

        assertIs<Token.Text>(statement3.tokens[0])
        assertEquals("!", (statement3.tokens[0] as Token.Text).value)

        assertIs<Token.Text>(statement3.tokens[1])
        assertEquals("include", (statement3.tokens[1] as Token.Text).value)

        assertIs<Token.Text>(statement3.tokens[2])
        assertEquals("stuff", (statement3.tokens[2] as Token.Text).value)
    }

    @Test
    fun `test generated source references`() {
        val lexer = Lexer()
        val unit = CompilationUnit("test", "this is +1 @label\nanother \$variable;!include stuff")

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(3, tokenStatements.size, "Invalid number of statements")

        val token = tokenStatements[2].tokens[1]
        val source = SourceLink.CompilationUnitSlice(unit, 37, 1, 19, 7)

        assertEquals(source, token.source)
    }

    @Test
    fun `test namespaced keys`() {
        val lexer = Lexer()
        val unit = CompilationUnit("test", "namespace:folder/folder/value.thing\n2 / 3")

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(2, tokenStatements.size, "Invalid number of statements")

        assertEquals(1, tokenStatements[0].tokens.size, "Invalid number of tokens in statement 1")
        val token = tokenStatements[0].tokens[0]

        assertIs<Token.Text>(token)

        assertEquals("namespace:folder/folder/value.thing", token.value)

        assertEquals(3, tokenStatements[1].tokens.size, "Invalid number of tokens in statement 2")
    }
}
