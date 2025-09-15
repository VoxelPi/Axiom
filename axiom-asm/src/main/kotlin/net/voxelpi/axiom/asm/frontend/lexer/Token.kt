package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.Sourced

public sealed interface Token : Sourced {

    override val source: SourceReference.UnitSlice

    public sealed interface Separator : Token {

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
    ) : Token

    public data class Integer(
        public val value: Long,
        override val source: SourceReference.UnitSlice,
    ) : Token

    public data class Bracket(
        val type: BracketType,
        val tokens: List<Token>,
        val openingBracketSource: SourceReference.UnitSlice,
        val closingBracketSource: SourceReference.UnitSlice,
    ) : Token {

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
