package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.Token

public sealed interface ParserTransformationLiteral : ParserTransformationSegment {

    public data class Text(val value: String) : ParserTransformationLiteral {

        override fun isApplicable(token: Token): Boolean {
            return when (token) {
                is Token.Text -> token.value == value
                is Token.Integer -> token.source.text == value
                else -> false
            }
        }
    }

    public data class Directive(val value: String) : ParserTransformationLiteral {

        override fun isApplicable(token: Token): Boolean {
            return token is Token.Directive && token.value == value
        }
    }

    public sealed interface CurlyBrackets : ParserTransformationLiteral {

        public data object Open : CurlyBrackets {

            override fun isApplicable(token: Token): Boolean {
                return token is Token.CurlyBrackets.Open
            }
        }

        public data object Close : CurlyBrackets {

            override fun isApplicable(token: Token): Boolean {
                return token is Token.CurlyBrackets.Close
            }
        }
    }

    public sealed interface SquareBrackets : ParserTransformationLiteral {

        public data object Open : SquareBrackets {

            override fun isApplicable(token: Token): Boolean {
                return token is Token.SquareBrackets.Open
            }
        }

        public data object Close : SquareBrackets {

            override fun isApplicable(token: Token): Boolean {
                return token is Token.SquareBrackets.Close
            }
        }
    }
}
