package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.Token
import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.statement.argument.Argument
import net.voxelpi.axiom.instruction.Condition

public sealed interface ParserTransformationArgument<T> : ParserTransformationSegment {

    public val id: String

    override fun isApplicable(token: Token): Boolean {
        return true
    }

    public fun isValid(token: Token): Boolean

    public fun parse(token: Token): Result<T>

    public data class TextArgument(override val id: String) : ParserTransformationArgument<Argument.Text> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text
        }

        override fun parse(token: Token): Result<Argument.Text> {
            if (token !is Token.Text) {
                return Result.failure(ParseException(token.source, "Expected a text token but got ${token::class.simpleName}"))
            }
            return Result.success(Argument.Text(token.source, token.value))
        }
    }

    public data class IntegerArgument(override val id: String) : ParserTransformationArgument<Argument.Integer> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Integer
        }

        override fun parse(token: Token): Result<Argument.Integer> {
            if (token !is Token.Integer) {
                return Result.failure(ParseException(token.source, "Expected an integer token but got ${token::class.simpleName}"))
            }
            return Result.success(Argument.Integer(token.source, token.value))
        }
    }

    public data class VariableArgument(override val id: String) : ParserTransformationArgument<Argument.VariableLike.NamedVariableReference> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable
        }

        override fun parse(token: Token): Result<Argument.VariableLike.NamedVariableReference> {
            if (token !is Token.Variable) {
                return Result.failure(ParseException(token.source, "Expected a variable token but got ${token::class.simpleName}"))
            }
            return Result.success(Argument.VariableLike.NamedVariableReference(token.source, token.value))
        }
    }

    public data class LabelArgument(override val id: String) : ParserTransformationArgument<Argument.LabelLike.NamedLabelReference> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Label
        }

        override fun parse(token: Token): Result<Argument.LabelLike.NamedLabelReference> {
            if (token !is Token.Label) {
                return Result.failure(ParseException(token.source, "Expected a label token but got ${token::class.simpleName}"))
            }
            return Result.success(Argument.LabelLike.NamedLabelReference(token.source, token.value))
        }
    }

    public data class ScopeArgument(override val id: String) : ParserTransformationArgument<Argument.ScopeLike.NamedScopeReference> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Label
        }

        override fun parse(token: Token): Result<Argument.ScopeLike.NamedScopeReference> {
            if (token !is Token.Label) {
                return Result.failure(ParseException(token.source, "Expected a label token but got ${token::class.simpleName}"))
            }
            return Result.success(Argument.ScopeLike.NamedScopeReference(token.source, token.value))
        }
    }

    public data class UnitArgument(override val id: String) : ParserTransformationArgument<Argument.UnitLike.NamedUnitReference> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text
        }

        override fun parse(token: Token): Result<Argument.UnitLike.NamedUnitReference> {
            if (token !is Token.Text) {
                return Result.failure(ParseException(token.source, "Expected a text token but got ${token::class.simpleName}"))
            }
            return Result.success(Argument.UnitLike.NamedUnitReference(token.source, token.value))
        }
    }

    public data class ConditionArgument(override val id: String) : ParserTransformationArgument<Condition> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text && token.value in Condition.entries.map { it.name }
        }

        override fun parse(token: Token): Result<Condition> {
            if (token !is Token.Text) {
                return Result.failure(ParseException(token.source, "Expected a text token but got ${token::class.simpleName}"))
            }

            val condition = Condition.entries.find { it.symbol == token.value }
                ?: return Result.failure(ParseException(token.source, "Unknown condition '${token.value}'"))

            return Result.success(condition)
        }
    }

    public data class ValueLikeArgument(override val id: String) : ParserTransformationArgument<Argument.ValueLike> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable || token is Token.Text || token is Token.Integer
        }

        override fun parse(token: Token): Result<Argument.ValueLike> {
            return when (token) {
                is Token.Variable -> Result.success(Argument.Variable(token.source, token.value))
                is Token.Text -> Result.success(Argument.ValueLike.Unparsed(token.source, token.value))
                is Token.Integer -> Result.success(Argument.Integer(token.source, token.value))
                else -> Result.failure(
                    ParseException(token.source, "Expected a variable, text or integer token but got ${token::class.simpleName}"),
                )
            }
        }
    }

    public data class RegisterLikeArgument(override val id: String) : ParserTransformationArgument<Argument.RegisterLike> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable || token is Token.Text
        }

        override fun parse(token: Token): Result<Argument.RegisterLike> {
            return when (token) {
                is Token.Variable -> Result.success(Argument.Variable(token.source, token.value))
                is Token.Text -> Result.success(Argument.RegisterLike.Unparsed(token.source, token.value))
                else -> Result.failure(
                    ParseException(token.source, "Expected a variable, text or integer token but got ${token::class.simpleName}"),
                )
            }
        }
    }
}
