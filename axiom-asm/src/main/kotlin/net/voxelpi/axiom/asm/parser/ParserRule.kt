package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement

internal data class ParserRule(
    val id: String
) {

    fun isApplicable(statement: TokenizedStatement): Boolean {
        TODO()
    }

    fun apply(statement: TokenizedStatement): Result<Statement> {
        TODO()
    }


    companion object {
        fun create(id: String, block: Builder.() -> Unit): ParserRule {
            val builder = Builder(id)
            builder.block()
            TODO()
        }
    }

    class Builder internal constructor(
        val id: String,
    ) {

        private var action: ArgumentState.() -> Statement = { TODO() }
        private val parts: MutableList<ParserRulePart> = mutableListOf()
        private val arguments: MutableMap<String, ParserRulePart.Argument<*>> = mutableMapOf()

        fun literal(text: String): ParserRulePart {
            val part = ParserRulePart.Literal(text)
            parts += part
            return part
        }

        fun directive(text: String): ParserRulePart {
            val part = ParserRulePart.Directive(text)
            parts += part
            return part
        }

        fun textArgument(id: String): ParserRulePart.Argument.Text {
            val part = ParserRulePart.Argument.Text(id)
            parts += part
            arguments[id] = part
            return part
        }

        fun integerArgument(id: String): ParserRulePart.Argument.Integer {
            val part = ParserRulePart.Argument.Integer(id)
            parts += part
            arguments[id] = part
            return part
        }

        fun variableArgument(id: String): ParserRulePart.Argument.Variable{
            val part = ParserRulePart.Argument.Variable(id)
            parts += part
            arguments[id] = part
            return part
        }

        fun labelArgument(id: String): ParserRulePart.Argument.Label {
            val part = ParserRulePart.Argument.Label(id)
            parts += part
            arguments[id] = part
            return part
        }

        fun curlyBracketsOpen(): ParserRulePart {
            val part = ParserRulePart.CurlyBrackets.Open
            parts += part
            return part
        }

        fun curlyBracketsClose(): ParserRulePart {
            val part = ParserRulePart.CurlyBrackets.Close
            parts += part
            return part
        }

        fun squareBracketsOpen(): ParserRulePart {
            val part = ParserRulePart.SquareBrackets.Open
            parts += part
            return part
        }

        fun squareBracketsClose(): ParserRulePart {
            val part = ParserRulePart.SquareBrackets.Close
            parts += part
            return part
        }

        fun then(action: ArgumentState.() -> Statement) {
            this.action = action
        }
    }

    data class ArgumentState(val argumentState: Map<String, *>) {

        @Suppress("UNCHECKED_CAST")
        fun <T> valueOf(argument: ParserRulePart.Argument<T>): T {
            return argumentState[argument.id] as T
        }

        fun sourceOf(argument: ParserRulePart): SourceLink {
            TODO()
        }

        fun statementSource(): SourceLink {
            TODO()
        }
    }
}
