package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.argument.Argument
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserTest {

    class SimpleStatement(
        override val source: SourceLink,
        val number: Argument.Integer,
    ) : Statement

    @Test
    fun `test simple parser`() {
        val lexer = Lexer()
        val parser = Parser.create {
            transformation<SimpleStatement>("simple statement") {
                literal("simple")
                integerArgument("number")
            }
        }

        val source = CompilationUnit("__test__", "simple 5")
        val tokenizedStatement = lexer.tokenize(source).first()

        val statement = parser.parse(tokenizedStatement).getOrThrow()
        assertIs<SimpleStatement>(statement)
        assertEquals(5, statement.number.value)
    }

    @Test
    fun `test parser type checks`() {
        val lexer = Lexer()
        val parser = Parser.create {
            transformation<SimpleStatement>("simple statement") {
                literal("simple")
                textArgument("number")
            }
        }

        val source = CompilationUnit("__test__", "simple test")
        val tokenizedStatement = lexer.tokenize(source).first()

        assertThrows<ParseException> { parser.parse(tokenizedStatement).getOrThrow() }
    }

    @Test
    fun `test complex parser`() {
        val code = """
        @my_scope {
            !include @lib_scope from my_lib
            R1 = R2 + R3 if R1 < 0
        }
        """.trimIndent()
        val unit = CompilationUnit("__test__", code)

        val lexer = Lexer()
        val parser = Parsers.AXIOM_ASM

        val tokenizedStatements = lexer.tokenize(unit)
        for (tokenizedStatement in tokenizedStatements) {
            val statement = parser.parse(tokenizedStatement).getOrThrow()
        }
    }
}
