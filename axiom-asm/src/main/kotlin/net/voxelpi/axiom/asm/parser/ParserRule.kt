package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement
import kotlin.reflect.KClass

public class ParserRule<S : Any> internal constructor(
    public val id: String,
    public val type: KClass<S>,
    public val parts: List<ParserRulePart>,
    public val arguments: Map<String, ParserRulePart.Argument<*>>,
) {

    public fun isApplicable(statement: TokenizedStatement): Boolean {
        TODO()
    }

    public fun apply(statement: TokenizedStatement): Result<Statement> {
        TODO()
    }

    internal companion object {
        fun <S : Any> create(id: String, type: KClass<S>, block: Builder<S>.() -> Unit): ParserRule<S> {
            val builder = Builder(id, type)
            builder.block()
            return ParserRule(builder.id, type, builder.parts(), builder.arguments())
        }

        inline fun <reified S : Any> create(id: String, noinline block: Builder<S>.() -> Unit): ParserRule<S> {
            return create(id, S::class, block)
        }
    }

    public class Builder<S : Any> internal constructor(
        public val id: String,
        public val type: KClass<S>,
    ) {
        private val parts: MutableList<ParserRulePart> = mutableListOf()
        private val arguments: MutableMap<String, ParserRulePart.Argument<*>> = mutableMapOf()

        public fun parts(): List<ParserRulePart> {
            return parts
        }

        public fun arguments(): Map<String, ParserRulePart.Argument<*>> {
            return arguments
        }

        public fun literal(text: String): ParserRulePart {
            val part = ParserRulePart.Literal(text)
            parts += part
            return part
        }

        public fun directive(text: String): ParserRulePart {
            val part = ParserRulePart.Directive(text)
            parts += part
            return part
        }

        public fun textArgument(id: String): ParserRulePart.Argument.Text {
            val part = ParserRulePart.Argument.Text(id)
            parts += part
            arguments[id] = part
            return part
        }

        public fun integerArgument(id: String): ParserRulePart.Argument.Integer {
            val part = ParserRulePart.Argument.Integer(id)
            parts += part
            arguments[id] = part
            return part
        }

        public fun variableArgument(id: String): ParserRulePart.Argument.Variable {
            val part = ParserRulePart.Argument.Variable(id)
            parts += part
            arguments[id] = part
            return part
        }

        public fun labelArgument(id: String): ParserRulePart.Argument.Label {
            val part = ParserRulePart.Argument.Label(id)
            parts += part
            arguments[id] = part
            return part
        }

        public fun curlyBracketsOpen(): ParserRulePart {
            val part = ParserRulePart.CurlyBrackets.Open
            parts += part
            return part
        }

        public fun curlyBracketsClose(): ParserRulePart {
            val part = ParserRulePart.CurlyBrackets.Close
            parts += part
            return part
        }

        public fun squareBracketsOpen(): ParserRulePart {
            val part = ParserRulePart.SquareBrackets.Open
            parts += part
            return part
        }

        public fun squareBracketsClose(): ParserRulePart {
            val part = ParserRulePart.SquareBrackets.Close
            parts += part
            return part
        }
    }
}
