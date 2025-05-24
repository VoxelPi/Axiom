package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.TokenizedStatement
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.LabelLike
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.asm.type.VariableLike
import net.voxelpi.axiom.instruction.Condition

public class ParserTransformation internal constructor(
    public val id: String,
    public val statement: Statement,
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
    public fun apply(tokenizedStatement: TokenizedStatement, scope: Scope): Result<StatementInstance> {
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

        val prototype = StatementInstance(statement, tokenizedStatement.source, statementParameterValues, statementParameterSources, scope)

        // // Try building the statement.
        // // This checks if all types are valid.
        // prototype.build().onFailure {
        //     return Result.failure(ParseException(tokenizedStatement.source, "Failed to build statement prototype: ${it.message}"))
        // }

        // Return the created statement prototype.
        return Result.success(prototype)
    }

    internal companion object {
        fun create(id: String, statement: Statement, block: Builder.() -> Unit): ParserTransformation {
            val builder = Builder(id, statement)
            builder.block()
            return ParserTransformation(builder.id, builder.statement, builder.segments(), builder.arguments(), builder.parameters())
        }
    }

    public class Builder internal constructor(
        public val id: String,
        public val statement: Statement,
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
            arguments[id] = argument
            return argument
        }

        public fun integerArgument(parameter: StatementParameter<IntegerValue>): ParserTransformationArgument.IntegerArgument {
            val argument = ParserTransformationArgument.IntegerArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun variableArgument(parameter: StatementParameter<in VariableLike>): ParserTransformationArgument.VariableArgument {
            val argument = ParserTransformationArgument.VariableArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun labelArgument(parameter: StatementParameter<in LabelLike>): ParserTransformationArgument.LabelArgument {
            val argument = ParserTransformationArgument.LabelArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun scopeNameArgument(parameter: StatementParameter<ScopeLike.ScopeName>): ParserTransformationArgument.ScopeArgument {
            val argument = ParserTransformationArgument.ScopeArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun scopeArgument(parameter: StatementParameter<in ScopeLike>): ParserTransformationArgument.ScopeArgument {
            val argument = ParserTransformationArgument.ScopeArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun unitArgument(parameter: StatementParameter<in UnitLike>): ParserTransformationArgument.UnitArgument {
            val argument = ParserTransformationArgument.UnitArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun conditionArgument(parameter: StatementParameter<Condition>): ParserTransformationArgument.ConditionArgument {
            val argument = ParserTransformationArgument.ConditionArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun valueLikeArgument(parameter: StatementParameter<in ValueLike>): ParserTransformationArgument.ValueLikeArgument {
            val argument = ParserTransformationArgument.ValueLikeArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun registerLikeArgument(parameter: StatementParameter<in RegisterLike>): ParserTransformationArgument.RegisterLikeArgument {
            val argument = ParserTransformationArgument.RegisterLikeArgument(parameter)
            segments += argument
            arguments[id] = argument
            return argument
        }

        public fun <T> parameter(parameter: StatementParameter<T>, value: ArgumentState.() -> T) {
            val parameter = ParserTransformationParameter(parameter, value)
            parameters[id] = parameter
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
