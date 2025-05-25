package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.lexer.TokenizedStatement
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.LabelLike
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.asm.type.VariableLike
import net.voxelpi.axiom.instruction.Condition
import kotlin.reflect.KProperty

public class ParserTransformation<T : Any> internal constructor(
    public val id: String,
    public val statement: StatementPrototype<T>,
    public val segments: List<ParserTransformationSegment>,
    public val arguments: Map<String, ParserTransformationArgument<*>>,
    public val parameters: Map<String, ParserTransformationParameter<*>>,
) {

    init {
        for (parameter in statement.parameters.values) {
            if (parameter.id !in arguments && parameter.id !in parameters) {
                throw IllegalArgumentException("The parameter '${parameter.id}' of the statement '${statement.id}' is not defined in the transformation.")
            }
        }
    }

    /**
     * Checks if this rule can be applied to the given [statement].
     */
    public fun isApplicable(statement: TokenizedStatement): Boolean {
        val tokens = statement.tokens
        if (tokens.size != segments.size) {
            return false
        }

        val includeArgumentTypes = segments
            .filterIsInstance<ParserTransformationLiteral>()
            .none()

        return segments.zip(tokens).all { (part, token) ->
            if (includeArgumentTypes && part is ParserTransformationArgument<*> && !part.isValid(token)) {
                return false
            }
            part.isApplicable(token)
        }
    }

    /**
     * Applies this rule on the given [tokenizedStatement] to generate a parsed statement.
     */
    public fun apply(tokenizedStatement: TokenizedStatement, scope: Scope): Result<StatementInstance<*>> {
        val tokens = tokenizedStatement.tokens

        val statementParameterValues = mutableMapOf<String, Any?>()
        val statementParameterSources = mutableMapOf<String, SourceLink>()

        // Parse argument values.
        for ((segment, token) in segments.zip(tokens)) {
            if (segment !is ParserTransformationArgument<*>) {
                continue
            }

            val argument = arguments[segment.parameter.id]!!
            val value = argument.parse(token).getOrElse {
                return Result.failure(it)
            }
            statementParameterValues[argument.parameter.id] = value.value
            statementParameterSources[argument.parameter.id] = value.source
        }

        // Set parameters.
        val argumentState = ArgumentState(statementParameterValues)
        for (parameter in parameters.values) {
            statementParameterValues[parameter.parameter.id] = parameter.valueProvider(argumentState)
        }

        // Create an instance of the statement.
        val instance = runCatching { StatementInstance(statement, scope, tokenizedStatement.source, statementParameterValues, statementParameterSources) }
            .getOrElse { return Result.failure(ParseException(tokenizedStatement.source, "Failed to create statement instance. ${it.message ?: ""} (${it::class.simpleName})")) }

        // Return the created statement instance.
        return Result.success(instance)
    }

    internal companion object {
        fun <T : Any> create(id: String, statement: StatementPrototype<T>, block: Builder<T>.() -> Unit): ParserTransformation<T> {
            val builder = Builder(id, statement)
            builder.block()
            return ParserTransformation(builder.id, builder.statement, builder.segments(), builder.arguments(), builder.parameters())
        }
    }

    public class Builder<T : Any> internal constructor(
        public val id: String,
        public val statement: StatementPrototype<T>,
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

        public fun textArgument(parameter: StatementParameter<String>): ParserTransformationArgument.TextArgument {
            val argument = ParserTransformationArgument.TextArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun textArgument(property: KProperty<String>): ParserTransformationArgument.TextArgument {
            return textArgument(StatementParameter.create(property))
        }

        public fun integerArgument(parameter: StatementParameter<IntegerValue>): ParserTransformationArgument.IntegerArgument {
            val argument = ParserTransformationArgument.IntegerArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun integerArgument(property: KProperty<IntegerValue>): ParserTransformationArgument.IntegerArgument {
            return integerArgument(StatementParameter.create(property))
        }

        public fun variableArgument(parameter: StatementParameter<in VariableLike>): ParserTransformationArgument.VariableArgument {
            val argument = ParserTransformationArgument.VariableArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun variableArgument(property: KProperty<VariableLike>): ParserTransformationArgument.VariableArgument {
            return variableArgument(StatementParameter.create(property))
        }

        public fun labelArgument(parameter: StatementParameter<in LabelLike>): ParserTransformationArgument.LabelArgument {
            val argument = ParserTransformationArgument.LabelArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun labelArgument(property: KProperty<LabelLike>): ParserTransformationArgument.LabelArgument {
            return labelArgument(StatementParameter.create(property))
        }

        public fun scopeArgument(parameter: StatementParameter<in ScopeLike>): ParserTransformationArgument.ScopeArgument {
            val argument = ParserTransformationArgument.ScopeArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun scopeArgument(property: KProperty<ScopeLike>): ParserTransformationArgument.ScopeArgument {
            return scopeArgument(StatementParameter.create(property))
        }

        public fun unitArgument(parameter: StatementParameter<in UnitLike>): ParserTransformationArgument.UnitArgument {
            val argument = ParserTransformationArgument.UnitArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun unitArgument(property: KProperty<UnitLike>): ParserTransformationArgument.UnitArgument {
            return unitArgument(StatementParameter.create(property))
        }

        public fun conditionArgument(parameter: StatementParameter<Condition>): ParserTransformationArgument.ConditionArgument {
            val argument = ParserTransformationArgument.ConditionArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun conditionArgument(property: KProperty<Condition>): ParserTransformationArgument.ConditionArgument {
            return conditionArgument(StatementParameter.create(property))
        }

        public fun valueLikeArgument(parameter: StatementParameter<in ValueLike>): ParserTransformationArgument.ValueLikeArgument {
            val argument = ParserTransformationArgument.ValueLikeArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun valueLikeArgument(property: KProperty<ValueLike>): ParserTransformationArgument.ValueLikeArgument {
            return valueLikeArgument(StatementParameter.create(property))
        }

        public fun registerLikeArgument(parameter: StatementParameter<in RegisterLike>): ParserTransformationArgument.RegisterLikeArgument {
            val argument = ParserTransformationArgument.RegisterLikeArgument(parameter)
            segments += argument
            arguments[parameter.id] = argument
            return argument
        }

        public fun registerLikeArgument(property: KProperty<RegisterLike>): ParserTransformationArgument.RegisterLikeArgument {
            return registerLikeArgument(StatementParameter.create(property))
        }

        public fun <T> parameter(parameter: StatementParameter<T>, value: ArgumentState.() -> T) {
            val transformationParameter = ParserTransformationParameter(parameter, value)
            parameters[parameter.id] = transformationParameter
        }

        public inline fun <reified T> parameter(property: KProperty<T>, noinline value: ArgumentState.() -> T) {
            return parameter(StatementParameter.create(property), value)
        }
    }

    public class ArgumentState internal constructor(private val arguments: Map<String, Any?>) {

        public operator fun get(parameter: StatementParameter<*>): Any? {
            return arguments[parameter.id]
        }

        @Suppress("UNCHECKED_CAST")
        public operator fun <T> get(argument: ParserTransformationArgument<T>): T {
            return arguments[argument.parameter.id]!! as T
        }
    }
}
