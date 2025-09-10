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

        val token21 = tokenStatements[2].tokens[1]
        val source21 = SourceLink.CompilationUnitSlice(unit, 37, 1, 19, 7)
        assertEquals(source21, token21.source)
        assertEquals("include", source21.text)

        val token22 = tokenStatements[2].tokens[2]
        val source22 = SourceLink.CompilationUnitSlice(unit, 45, 1, 27, 5)
        assertEquals(source22, token22.source)
        assertEquals("stuff", source22.text)
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

    @Test
    fun `test complex characters variables`() {
        val lexer = Lexer()
        val unit = CompilationUnit("test", "$💀")

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(1, tokenStatements.size, "Invalid number of statements")

        val variableStatement = tokenStatements[0]
        assertEquals(1, variableStatement.tokens.size, "Invalid number of tokens in statement")
        assertIs<Token.Variable>(variableStatement.tokens[0])
    }

    @Test
    fun `test complex characters`() {
        val lexer = Lexer()
        val unit = CompilationUnit("test", "💀\n$💀\n@💀")

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(3, tokenStatements.size, "Invalid number of statements")

        val literalStatement = tokenStatements[0]
        assertEquals(1, literalStatement.tokens.size, "Invalid number of tokens in statement 1")
        assertIs<Token.Text>(literalStatement.tokens[0])

        val variableStatement = tokenStatements[1]
        assertEquals(1, variableStatement.tokens.size, "Invalid number of tokens in statement 2")
        assertIs<Token.Variable>(variableStatement.tokens[0])
    }

    @Test
    fun `test string tokens`() {
        val lexer = Lexer()
        val unit = CompilationUnit(
            "test",
            """
            "text"
            """.trimIndent()
        )

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(1, tokenStatements.size, "Invalid number of statements")
        assertEquals(1, tokenStatements[0].tokens.size, "Invalid number of tokens in statement 1")
        assertIs<Token.StringText>(tokenStatements[0].tokens[0])

        val textToken = tokenStatements[0].tokens[0] as Token.StringText
        assertEquals("text", textToken.value)
    }

    @Test
    fun `test string tokens with semicolons`() {
        val testText = " 1 ; 2 "

        val lexer = Lexer()
        val unit = CompilationUnit(
            "test",
            """
            "$testText"
            """.trimIndent()
        )

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(1, tokenStatements.size, "Invalid number of statements")
        val statement = tokenStatements[0]
        assertEquals(1, statement.tokens.size, "Invalid number of tokens in statement 1")

        val textToken = tokenStatements[0].tokens[0] as Token.StringText
        assertEquals(testText, textToken.value)
    }

    @Test
    fun `test escaped tokens`() {
        val lexer = Lexer()
        val unit = CompilationUnit(
            "test",
            """
            "\b\n\r\t"
            """.trimIndent()
        )

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(1, tokenStatements.size, "Invalid number of statements")
        val statement = tokenStatements[0]
        assertEquals(1, statement.tokens.size, "Invalid number of tokens in statement 1")

        val textToken = tokenStatements[0].tokens[0] as Token.StringText
        assertEquals("\b\n\r\t", textToken.value)
    }

    @Test
    fun `test characters`() {
        val lexer = Lexer()
        val unit = CompilationUnit(
            "test",
            """
            'l'
            """.trimIndent()
        )

        val tokenStatements = lexer.tokenize(unit)
        assertEquals(1, tokenStatements.size, "Invalid number of statements")
        val statement = tokenStatements[0]
        assertEquals(1, statement.tokens.size, "Invalid number of tokens in statement 1")

        val textToken = tokenStatements[0].tokens[0] as Token.Integer
        assertEquals('l'.code.toLong(), textToken.value)
    }
}
