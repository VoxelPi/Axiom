package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement
import kotlin.reflect.KClass

public class Parser(
    public val rules: List<ParserTransformation<*>>,
) {

    public fun parse(statement: TokenizedStatement): Result<Statement> {
        for (rule in rules) {
            if (!rule.isApplicable(statement)) {
                continue
            }

            val statement = rule.apply(statement)
            return statement
        }

        return Result.failure(IllegalArgumentException("No applicable parsing rule found."))
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

        public fun <S : Statement> rule(id: String, type: KClass<S>, block: ParserTransformation.Builder<S>.() -> Unit): ParserTransformation<S> {
            val rule = ParserTransformation.create(id, type, block)
            rules += rule
            return rule
        }

        public inline fun <reified S : Statement> rule(id: String, noinline block: ParserTransformation.Builder<S>.() -> Unit): ParserTransformation<S> {
            return rule(id, S::class, block)
        }
    }
}
