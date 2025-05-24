package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.lexer.Token
import net.voxelpi.axiom.asm.statement.StatementParameter
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.LabelLike
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.asm.type.VariableLike
import net.voxelpi.axiom.instruction.Condition

public sealed interface ParserTransformationArgument<T> : ParserTransformationSegment {

    public val parameter: StatementParameter<in T>

    override fun isApplicable(token: Token): Boolean {
        return true
    }

    public fun isValid(token: Token): Boolean

    public fun parse(token: Token): Result<ParsedValue<out T>>

    public data class TextArgument(override val parameter: StatementParameter<String>) : ParserTransformationArgument<String> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text
        }

        override fun parse(token: Token): Result<ParsedValue<String>> {
            if (token !is Token.Text) {
                return Result.failure(ParseException(token.source, "Expected a text token but got ${token::class.simpleName}"))
            }
            return Result.success(ParsedValue.create(token.source, token.value))
        }
    }

    public data class IntegerArgument(override val parameter: StatementParameter<IntegerValue>) : ParserTransformationArgument<IntegerValue> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Integer
        }

        override fun parse(token: Token): Result<ParsedValue<IntegerValue>> {
            if (token !is Token.Integer) {
                return Result.failure(ParseException(token.source, "Expected an integer token but got ${token::class.simpleName}"))
            }
            return Result.success(ParsedValue.create(token.source, IntegerValue(token.value)))
        }
    }

    public data class VariableArgument(override val parameter: StatementParameter<in VariableLike>) : ParserTransformationArgument<VariableLike> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable
        }

        override fun parse(token: Token): Result<ParsedValue<VariableLike.VariableName>> {
            if (token !is Token.Variable) {
                return Result.failure(ParseException(token.source, "Expected a variable token but got ${token::class.simpleName}"))
            }
            return Result.success(ParsedValue.create(token.source, VariableLike.VariableName(token.value)))
        }
    }

    public data class LabelArgument(override val parameter: StatementParameter<in LabelLike>) : ParserTransformationArgument<LabelLike> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Label
        }

        override fun parse(token: Token): Result<ParsedValue<LabelLike.LabelName>> {
            if (token !is Token.Label) {
                return Result.failure(ParseException(token.source, "Expected a label token but got ${token::class.simpleName}"))
            }
            return Result.success(ParsedValue.create(token.source, LabelLike.LabelName(token.value)))
        }
    }

    public data class ScopeArgument(override val parameter: StatementParameter<in ScopeLike.ScopeName>) : ParserTransformationArgument<ScopeLike.ScopeName> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Label
        }

        override fun parse(token: Token): Result<ParsedValue<ScopeLike.ScopeName>> {
            if (token !is Token.Label) {
                return Result.failure(ParseException(token.source, "Expected a label token but got ${token::class.simpleName}"))
            }
            return Result.success(ParsedValue.create(token.source, ScopeLike.ScopeName(token.value)))
        }
    }

    public data class UnitArgument(override val parameter: StatementParameter<in UnitLike.UnitName>) : ParserTransformationArgument<UnitLike.UnitName> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text
        }

        override fun parse(token: Token): Result<ParsedValue<UnitLike.UnitName>> {
            if (token !is Token.Text) {
                return Result.failure(ParseException(token.source, "Expected a text token but got ${token::class.simpleName}"))
            }
            return Result.success(ParsedValue.create(token.source, UnitLike.UnitName(token.value)))
        }
    }

    public data class ConditionArgument(override val parameter: StatementParameter<Condition>) : ParserTransformationArgument<Condition> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text && token.value in Condition.entries.map { it.name }
        }

        override fun parse(token: Token): Result<ParsedValue<Condition>> {
            if (token !is Token.Text) {
                return Result.failure(ParseException(token.source, "Expected a text token but got ${token::class.simpleName}"))
            }

            val condition = Condition.entries.find { it.symbol == token.value }
                ?: return Result.failure(ParseException(token.source, "Unknown condition '${token.value}'"))

            return Result.success(ParsedValue.create(token.source, condition))
        }
    }

    public data class ValueLikeArgument(override val parameter: StatementParameter<in ValueLike>) : ParserTransformationArgument<ValueLike> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable || token is Token.Text || token is Token.Integer
        }

        override fun parse(token: Token): Result<ParsedValue<ValueLike>> {
            val value: ValueLike = when (token) {
                is Token.Variable -> VariableLike.VariableName(token.value)
                is Token.Text -> ValueLike.UnparsedValue(token.value)
                is Token.Integer -> IntegerValue(token.value)
                else -> return Result.failure(
                    ParseException(token.source, "Expected a variable, text or integer token but got ${token::class.simpleName}"),
                )
            }
            return Result.success(ParsedValue.create(token.source, value))
        }
    }

    public data class RegisterLikeArgument(override val parameter: StatementParameter<in RegisterLike>) : ParserTransformationArgument<RegisterLike> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable || token is Token.Text
        }

        override fun parse(token: Token): Result<ParsedValue<RegisterLike>> {
            val value: RegisterLike = when (token) {
                is Token.Variable -> VariableLike.VariableName(token.value)
                is Token.Text -> RegisterLike.UnparsedRegister(token.value)
                else -> return Result.failure(
                    ParseException(token.source, "Expected a variable, text or integer token but got ${token::class.simpleName}"),
                )
            }
            return Result.success(ParsedValue.create(token.source, value))
        }
    }
}
