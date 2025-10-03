package net.voxelpi.axiom.asm.compositor.token

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.token.Token

internal sealed interface CompositorToken : Token {

    data class Separator(
        val type: BracketType,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class Symbol(
        val symbol: String,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class Text(
        val value: String,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class Integer(
        val value: Long,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class StringLiteral(
        val value: String,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class Placeholder(
        val id: NamespacedId,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class Bracket(
        val type: BracketType,
        val tokens: List<LexerToken>,
        val openingBracketSource: SourceReference.UnitSlice,
        val closingBracketSource: SourceReference.UnitSlice,
    ) : CompositorToken {

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
