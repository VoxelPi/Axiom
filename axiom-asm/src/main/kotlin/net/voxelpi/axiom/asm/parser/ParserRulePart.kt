package net.voxelpi.axiom.asm.parser

public sealed interface ParserRulePart {

    public data class Directive(val directive: String) : ParserRulePart

    public data class Literal(val value: String) : ParserRulePart

    public sealed interface Argument<T> : ParserRulePart {

        public val id: String

        public data class Text(override val id: String) : Argument<String>

        public data class Integer(override val id: String) : Argument<Long>

        public data class Variable(override val id: String) : Argument<String>

        public data class Label(override val id: String) : Argument<String>
    }

    public sealed interface CurlyBrackets : ParserRulePart {

        public data object Open : CurlyBrackets

        public data object Close : CurlyBrackets
    }

    public sealed interface SquareBrackets : ParserRulePart {

        public data object Open : SquareBrackets

        public data object Close : SquareBrackets
    }
}
