package net.voxelpi.axiom.asm.parser

internal sealed interface ParserRulePart {

    data class Directive(val directive: String) : ParserRulePart

    data class Literal(val value: String) : ParserRulePart

    sealed interface Argument<T> : ParserRulePart {

        val id: String

        data class Text(override val id: String) : Argument<String>

        data class Integer(override val id: String) : Argument<Long>

        data class Variable(override val id: String) : Argument<String>

        data class Label(override val id: String) : Argument<String>
    }

    sealed interface CurlyBrackets : ParserRulePart {
        data object Open : CurlyBrackets
        data object Close : CurlyBrackets
    }

    sealed interface SquareBrackets : ParserRulePart {
        data object Open : SquareBrackets
        data object Close : SquareBrackets
    }
}
