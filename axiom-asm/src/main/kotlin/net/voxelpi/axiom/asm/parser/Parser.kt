package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement
import kotlin.reflect.KClass

public class Parser(
    public val rules: List<ParserRule<*>>,
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

        private val rules: MutableList<ParserRule<*>> = mutableListOf()

        public fun rules(): List<ParserRule<*>> {
            return rules
        }

        public fun <S : Any> rule(id: String, type: KClass<S>, block: ParserRule.Builder<S>.() -> Unit): ParserRule<S> {
            val rule = ParserRule.create(id, type, block)
            rules += rule
            return rule
        }

        public inline fun <reified S : Any> rule(id: String, noinline block: ParserRule.Builder<S>.() -> Unit): ParserRule<S> {
            return rule(id, S::class, block)
        }
    }
}
