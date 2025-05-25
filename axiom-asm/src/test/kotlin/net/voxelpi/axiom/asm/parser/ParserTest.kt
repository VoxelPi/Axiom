package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.type.IntegerValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserTest {

    @StatementType("simple")
    class SimpleStatement(
        val number: IntegerValue,
    )

    val statementPrototype = StatementPrototype.fromType(SimpleStatement::class)

    @Test
    fun `test simple parser`() {
        val lexer = Lexer()
        val parser = Parser.create {
            transformation<SimpleStatement>("simple statement") {
                literal("simple")
                integerArgument(SimpleStatement::number)
            }
        }

        val source = CompilationUnit("__test__", "simple 5")
        val tokenizedStatement = lexer.tokenize(source).first()

        val globalScope = GlobalScope()

        val statementInstance = parser.parse(tokenizedStatement, globalScope).getOrThrow()
        val statement = statementInstance.create()
        assertIs<SimpleStatement>(statement)
        assertEquals(5, statement.number.value)
    }

    @Test
    fun `test missing statement parameter`() {
        assertThrows<Exception> {
            Parser.create {
                transformation<SimpleStatement>("simple statement") {
                    literal("simple")
                    // Nothing defined for the number parameter.
                }
            }
        }
    }

    @Test
    fun `test parser type checks`() {
        val lexer = Lexer()
        val parser = Parser.create {
            transformation<SimpleStatement>("simple statement") {
                literal("simple")
                textArgument(StatementParameter.create<String>("number"))
            }
        }

        val globalScope = GlobalScope()

        val source = CompilationUnit("__test__", "simple test")
        val tokenizedStatement = lexer.tokenize(source).first()

        assertThrows<ParseException> { parser.parse(tokenizedStatement, globalScope).getOrThrow() }
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

        val globalScope = GlobalScope()

        val tokenizedStatements = lexer.tokenize(unit)
        for (tokenizedStatement in tokenizedStatements) {
            parser.parse(tokenizedStatement, globalScope).getOrThrow()
        }
    }
}
