package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement
import net.voxelpi.axiom.asm.statement.argument.Argument
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor
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
    public fun apply(tokenizedStatement: TokenizedStatement): Result<Statement> {
        val tokens = tokenizedStatement.tokens

        val argumentValues = mutableMapOf<String, Any?>("source" to tokenizedStatement.source)
        val argumentState = ArgumentState(argumentValues)

        // Parse argument values.
        for ((segment, token) in segments.zip(tokens)) {
            if (segment !is ParserTransformationArgument<*>) {
                continue
            }

            val argument = arguments[segment.id]!!
            val value = argument.parse(token).getOrElse {
                return Result.failure(it)
            }
            argumentValues[argument.id] = value
        }

        // Set parameters.
        for (parameter in parameters.values) {
            argumentValues[parameter.id] = parameter.valueProvider(argumentState)
        }

        // Get the primary constructor.
        val primaryConstructor = type.primaryConstructor
            ?: return Result.failure(ParseException(tokenizedStatement.source, "No primary constructor found for statement class $type."))

        // Get constructor arguments of the primary constructor.
        val constructorArguments = primaryConstructor.parameters
            .filterNot { it.isOptional || it.isVararg }
            .associate { parameter ->
                val name = parameter.name ?: return Result.failure(ParseException(tokenizedStatement.source, "Constructor parameter '$parameter' has no name."))
                val type = parameter.type
                name to type
            }

        // Check the argument types.
        for ((argumentName, argumentType) in constructorArguments) {
            // Check that the argument is present in the value map.
            if (argumentName !in argumentValues) {
                return Result.failure(ParseException(tokenizedStatement.source, "No value specified for argument $argumentName."))
            }
            val argumentValue = argumentValues[argumentName]

            // Check that the type is valid.
            if (!isInstanceOfType(argumentValue, argumentType)) {
                val source = if (argumentValue is Argument) argumentValue.source else tokenizedStatement.source
                return Result.failure(ParseException(source, "Value '$argumentValue' for argument $argumentName is not of type $argumentType"))
            }
        }

        // Create the statement.
        val constructorValues = primaryConstructor.parameters.associateWith { parameter ->
            argumentValues[parameter.name!!]
        }
        return runCatching { primaryConstructor.callBy(constructorValues) }
    }

    private fun isInstanceOfType(value: Any?, type: KType): Boolean {
        // Handle null values: only return true if 'value' is nullable
        if (value == null) return type.isMarkedNullable

        return when (val classifier = type.classifier) {
            is KClass<*> -> classifier.isSuperclassOf(value::class)
            else -> classifier == value::class
        }
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

        public fun <T> parameter(id: String, type: KType, value: ArgumentState.() -> T): ParserTransformationParameter<T> {
            val parameter = ParserTransformationParameter(id, type, value)
            parameters[id] = parameter
            return parameter
        }

        public inline fun <reified T> parameter(id: String, noinline value: ArgumentState.() -> T): ParserTransformationParameter<T> {
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
