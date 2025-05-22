package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.TokenizedStatement
import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementPrototype
import kotlin.reflect.KClass

public class Parser(
    public val transformations: List<ParserTransformation<*>>,
) {

    public fun parse(statement: TokenizedStatement, scope: Scope): Result<StatementPrototype<*>> {
        for (rule in transformations) {
            if (!rule.isApplicable(statement)) {
                continue
            }

            val statement = rule.apply(statement, scope)
            return statement
        }

        return Result.failure(ParseException(statement.source, "No applicable parsing rule found."))
    }

    public companion object {

        public fun create(block: Builder.() -> Unit): Parser {
            val builder = Builder()
            builder.block()
            return Parser(builder.rules())
        }
    }

    public class Builder internal constructor() {

        private val rules: MutableList<ParserTransformation<*>> = mutableListOf()

        public fun rules(): List<ParserTransformation<*>> {
            return rules
        }

        public fun <S : Statement> transformation(id: String, type: KClass<S>, block: ParserTransformation.Builder<S>.() -> Unit): ParserTransformation<S> {
            val rule = ParserTransformation.create(id, type, block)
            rules += rule
            return rule
        }

        public inline fun <reified S : Statement> transformation(id: String, noinline block: ParserTransformation.Builder<S>.() -> Unit): ParserTransformation<S> {
            return transformation(id, S::class, block)
        }
    }
}
