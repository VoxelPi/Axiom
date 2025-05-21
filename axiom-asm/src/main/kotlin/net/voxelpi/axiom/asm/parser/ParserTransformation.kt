package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public class ParserTransformation<S : Any> internal constructor(
    public val id: String,
    public val type: KClass<S>,
    public val segments: List<ParserTransformationSegment>,
    public val arguments: Map<String, ParserTransformationArgument<*>>,
    public val parameters: Map<String, ParserTransformationParameter<*>>,
) {

    /**
     * Checks if this rule can be applied to the given [statement].
     */
    public fun isApplicable(statement: TokenizedStatement): Boolean {
        val tokens = statement.tokens
        if (tokens.size != segments.size) {
            return false
        }

        return segments.zip(tokens).all { (part, token) -> part.isApplicable(token) }
    }

    /**
     * Applies this rule on the given [statement] to generate a parsed statement.
     */
    public fun apply(statement: TokenizedStatement): Result<Statement> {
        val tokens = statement.tokens

        val argumentValues = mutableMapOf<String, Any?>()

        // Set parameters.
        for (parameter in parameters.values) {
            argumentValues[parameter.id] = parameter.value
        }

        // Parse argument values.
        for ((segment, token) in segments.zip(tokens)) {
            if (segment !is ParserTransformationArgument<*>) {
                continue
            }

            val argument = arguments[segment.id]!!
            argumentValues[argument.id] = argument.parse(token)
        }

        TODO()
    }

    internal companion object {
        fun <S : Any> create(id: String, type: KClass<S>, block: Builder<S>.() -> Unit): ParserTransformation<S> {
            val builder = Builder(id, type)
            builder.block()
            return ParserTransformation(builder.id, type, builder.segments(), builder.arguments(), builder.parameters())
        }

        inline fun <reified S : Any> create(id: String, noinline block: Builder<S>.() -> Unit): ParserTransformation<S> {
            return create(id, S::class, block)
        }
    }

    public class Builder<S : Any> internal constructor(
        public val id: String,
        public val type: KClass<S>,
    ) {
        private val segments: MutableList<ParserTransformationSegment> = mutableListOf()
        private val arguments: MutableMap<String, ParserTransformationArgument<*>> = mutableMapOf()
        private val parameters: MutableMap<String, ParserTransformationParameter<*>> = mutableMapOf()

        public fun segments(): List<ParserTransformationSegment> {
            return segments
        }

        public fun arguments(): Map<String, ParserTransformationArgument<*>> {
            return arguments
        }

        public fun parameters(): Map<String, ParserTransformationParameter<*>> {
            return parameters
        }

        public fun literal(text: String): ParserTransformationLiteral.Text {
            val part = ParserTransformationLiteral.Text(text)
            segments += part
            return part
        }

        public fun directive(text: String): ParserTransformationLiteral.Directive {
            val part = ParserTransformationLiteral.Directive(text)
            segments += part
            return part
        }

        public fun curlyBracketsOpen(): ParserTransformationLiteral.CurlyBrackets.Open {
            val part = ParserTransformationLiteral.CurlyBrackets.Open
            segments += part
            return part
        }

        public fun curlyBracketsClose(): ParserTransformationLiteral.CurlyBrackets.Close {
            val part = ParserTransformationLiteral.CurlyBrackets.Close
            segments += part
            return part
        }

        public fun squareBracketsOpen(): ParserTransformationLiteral.SquareBrackets.Open {
            val part = ParserTransformationLiteral.SquareBrackets.Open
            segments += part
            return part
        }

        public fun squareBracketsClose(): ParserTransformationLiteral.SquareBrackets.Close {
            val part = ParserTransformationLiteral.SquareBrackets.Close
            segments += part
            return part
        }

        public fun textArgument(id: String): ParserTransformationArgument.TextArgument {
            val argument = ParserTransformationArgument.TextArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun integerArgument(id: String): ParserTransformationArgument.IntegerArgument {
            val argument = ParserTransformationArgument.IntegerArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun variableArgument(id: String): ParserTransformationArgument.VariableArgument {
            val argument = ParserTransformationArgument.VariableArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun labelArgument(id: String): ParserTransformationArgument.LabelArgument {
            val argument = ParserTransformationArgument.LabelArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun conditionArgument(id: String): ParserTransformationArgument.ConditionArgument {
            val argument = ParserTransformationArgument.ConditionArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun valueLikeArgument(id: String): ParserTransformationArgument.ValueLikeArgument {
            val argument = ParserTransformationArgument.ValueLikeArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun registerLikeArgument(id: String): ParserTransformationArgument.RegisterLikeArgument {
            val argument = ParserTransformationArgument.RegisterLikeArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun <T> parameter(id: String, type: KType, value: T): ParserTransformationParameter<T> {
            val parameter = ParserTransformationParameter(id, type, value)
            parameters[id] = parameter
            return parameter
        }

        public inline fun <reified T> parameter(id: String, value: T): ParserTransformationParameter<T> {
            return parameter(id, typeOf<T>(), value)
        }
    }
}
