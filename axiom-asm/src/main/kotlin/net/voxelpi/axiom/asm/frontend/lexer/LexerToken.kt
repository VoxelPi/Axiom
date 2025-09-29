package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.frontend.token.Token
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.source.SourceReference

public sealed interface LexerToken : Token {

    override val source: SourceReference.UnitSlice

    public sealed interface Separator : LexerToken {

        public data class Weak(
            override val source: SourceReference.UnitSlice,
        ) : Separator

        public data class Normal(
            override val source: SourceReference.UnitSlice,
        ) : Separator

        public data class Strong(
            override val source: SourceReference.UnitSlice,
        ) : Separator
    }

    public data class Symbol(
        public val symbol: String,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class Text(
        public val value: String,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class Integer(
        public val value: Long,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class StringLiteral(
        public val value: String,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class Label(
        public val id: NamespacedId,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class Placeholder(
        public val id: NamespacedId,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class Directive(
        public val value: String,
        override val source: SourceReference.UnitSlice,
    ) : LexerToken

    public data class Bracket(
        val type: BracketType,
        val tokens: List<LexerToken>,
        val openingBracketSource: SourceReference.UnitSlice,
        val closingBracketSource: SourceReference.UnitSlice,
    ) : LexerToken {

        override val source: SourceReference.UnitSlice
            get() {
                require(openingBracketSource.unit == closingBracketSource.unit)
                return SourceReference.UnitSlice(
                    openingBracketSource.unit,
                    openingBracketSource.index,
                    closingBracketSource.index - openingBracketSource.index + 1,
                )
            }
    }
}
