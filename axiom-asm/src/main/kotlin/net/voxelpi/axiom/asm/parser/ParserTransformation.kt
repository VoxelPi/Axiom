package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.TokenizedStatement
import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementPrototype
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public class ParserTransformation<S : Statement> internal constructor(
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
     * Applies this rule on the given [tokenizedStatement] to generate a parsed statement.
     */
    public fun apply(tokenizedStatement: TokenizedStatement, scope: Scope): Result<StatementPrototype<*>> {
        val tokens = tokenizedStatement.tokens

        val statementArguments = mutableMapOf<String, Any?>()

        // Parse argument values.
        for ((segment, token) in segments.zip(tokens)) {
            if (segment !is ParserTransformationArgument<*>) {
                continue
            }

            val argument = arguments[segment.id]!!
            val value = argument.parse(token).getOrElse {
                return Result.failure(it)
            }
            statementArguments[argument.id] = value
        }

        // Set parameters.
        val argumentState = ArgumentState(statementArguments)
        for (parameter in parameters.values) {
            statementArguments[parameter.id] = parameter.valueProvider(argumentState)
        }
        val prototype = StatementPrototype(tokenizedStatement.source, type, scope, statementArguments)

        // Try building the statement.
        // This checks if all types are valid.
        prototype.build().onFailure {
            return Result.failure(ParseException(tokenizedStatement.source, "Failed to build statement prototype: ${it.message}"))
        }

        // Return the created statement prototype.
        return Result.success(prototype)
    }

    internal companion object {
        fun <S : Statement> create(id: String, type: KClass<S>, block: Builder<S>.() -> Unit): ParserTransformation<S> {
            val builder = Builder(id, type)
            builder.block()
            return ParserTransformation(builder.id, type, builder.segments(), builder.arguments(), builder.parameters())
        }

        inline fun <reified S : Statement> create(id: String, noinline block: Builder<S>.() -> Unit): ParserTransformation<S> {
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

        public fun literal(text: String) {
            val words = text.split("\\s+".toRegex())
            for (word in words) {
                val part = ParserTransformationLiteral.Text(word)
                segments += part
            }
        }

        public fun directive(text: String) {
            val part = ParserTransformationLiteral.Directive(text)
            segments += part
        }

        public fun curlyBracketsOpen() {
            val part = ParserTransformationLiteral.CurlyBrackets.Open
            segments += part
        }

        public fun curlyBracketsClose() {
            val part = ParserTransformationLiteral.CurlyBrackets.Close
            segments += part
        }

        public fun squareBracketsOpen() {
            val part = ParserTransformationLiteral.SquareBrackets.Open
            segments += part
        }

        public fun squareBracketsClose() {
            val part = ParserTransformationLiteral.SquareBrackets.Close
            segments += part
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

        public fun scopeArgument(id: String): ParserTransformationArgument.ScopeArgument {
            val argument = ParserTransformationArgument.ScopeArgument(id)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun unitArgument(id: String): ParserTransformationArgument.UnitArgument {
            val argument = ParserTransformationArgument.UnitArgument(id)
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

        public fun <T> parameter(id: String, type: KType, value: ArgumentState.() -> T) {
            val parameter = ParserTransformationParameter(id, type, value)
            parameters[id] = parameter
        }

        public inline fun <reified T> parameter(id: String, noinline value: ArgumentState.() -> T) {
            return parameter(id, typeOf<T>(), value)
        }
    }

    public class ArgumentState internal constructor(private val arguments: Map<String, Any?>) {

        public operator fun get(name: String): Any? {
            return arguments[name]
        }

        @Suppress("UNCHECKED_CAST")
        public operator fun <T> get(argument: ParserTransformationArgument<T>): T {
            return arguments[argument.id]!! as T
        }
    }
}
