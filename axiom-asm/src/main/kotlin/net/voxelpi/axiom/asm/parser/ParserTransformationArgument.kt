package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.Token
import net.voxelpi.axiom.asm.statement.argument.Argument
import net.voxelpi.axiom.instruction.Condition

public sealed interface ParserTransformationArgument<T> : ParserTransformationSegment {

    public val id: String

    override fun isApplicable(token: Token): Boolean {
        return true
    }

    public fun isValid(token: Token): Boolean

    public fun parse(token: Token): Result<T>

    public data class TextArgument(override val id: String) : ParserTransformationArgument<String> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text
        }

        override fun parse(token: Token): Result<String> {
            if (token !is Token.Text) {
                return Result.failure(IllegalArgumentException("Expected a text token but got ${token::class.simpleName}"))
            }
            return Result.success(token.value)
        }
    }

    public data class IntegerArgument(override val id: String) : ParserTransformationArgument<Long> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Integer
        }

        override fun parse(token: Token): Result<Long> {
            if (token !is Token.Integer) {
                return Result.failure(IllegalArgumentException("Expected an integer token but got ${token::class.simpleName}"))
            }
            return Result.success(token.value)
        }
    }

    public data class VariableArgument(override val id: String) : ParserTransformationArgument<String> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Variable
        }

        override fun parse(token: Token): Result<String> {
            if (token !is Token.Variable) {
                return Result.failure(IllegalArgumentException("Expected a variable token but got ${token::class.simpleName}"))
            }
            return Result.success(token.value)
        }
    }

    public data class LabelArgument(override val id: String) : ParserTransformationArgument<String> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Label
        }

        override fun parse(token: Token): Result<String> {
            if (token !is Token.Label) {
                return Result.failure(IllegalArgumentException("Expected a label token but got ${token::class.simpleName}"))
            }
            return Result.success(token.value)
        }
    }

    public data class ConditionArgument(override val id: String) : ParserTransformationArgument<Condition> {

        override fun isValid(token: Token): Boolean {
            return token is Token.Text && token.value in Condition.entries.map { it.name }
        }

        override fun parse(token: Token): Result<Condition> {
            if (token !is Token.Text) {
                return Result.failure(IllegalArgumentException("Expected a text token but got ${token::class.simpleName}"))
            }
            return Result.success(
                Condition.entries.find { it.name == token.value }
                    ?: return Result.failure(IllegalArgumentException("Unknown condition '${token.value}'")),
            )
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
                    IllegalArgumentException("Expected a variable, text or integer token but got ${token::class.simpleName}"),
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
                    IllegalArgumentException("Expected a variable, text or integer token but got ${token::class.simpleName}"),
                )
            }
        }
    }
}
