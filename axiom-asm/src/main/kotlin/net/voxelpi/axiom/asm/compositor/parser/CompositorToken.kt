package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.language.SeparatorType
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.SourcedValue
import net.voxelpi.axiom.asm.token.Token

internal sealed interface CompositorToken : Token {

    override val source: SourceReference.UnitSlice

    data class Separator(
        val type: SeparatorType,
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

    data class Label(
        val id: NamespacedId,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    data class Placeholder(
        val id: NamespacedId,
        override val source: SourceReference.UnitSlice,
    ) : CompositorToken

    sealed interface Directive : CompositorToken {

        data class Include(
            override val source: SourceReference.UnitSlice,
            val unitId: DirectiveParameterValue<String>,
        ) : Directive

        data class Define(
            override val source: SourceReference.UnitSlice,
            val id: NamespacedId,
            val value: List<CompositorToken>,
        ) : Directive

        data class Label(
            override val source: SourceReference.UnitSlice,
            val id: NamespacedId,
        ) : Directive

        data class Region(
            override val source: SourceReference.UnitSlice,
        ) : Directive

        data class Insert(
            override val source: SourceReference.UnitSlice,
            val template: DirectiveParameterValue<CompositorTemplateHeader>,
            val parameters: CompositorTemplatePrototype,
        ) : Directive

        data class If(
            override val source: SourceReference.UnitSlice,
            val condition: List<LexerToken>,
        ) : Directive

        data class Else(
            override val source: SourceReference.UnitSlice,
        ) : Directive

        data class Repeated(
            override val source: SourceReference.UnitSlice,
            val times: DirectiveParameterValue<ULong>,
        ) : Directive

        sealed interface Visibility : Directive {

            data class Global(
                override val source: SourceReference.UnitSlice,
            ) : Visibility

            data class Public(
                override val source: SourceReference.UnitSlice,
                val target: SourcedValue<NamespacedId>?,
            ) : Visibility

            data class Private(
                override val source: SourceReference.UnitSlice,
            ) : Visibility

            data class Inline(
                override val source: SourceReference.UnitSlice,
            ) : Visibility
        }

        sealed interface Location : Directive {

            data class At(
                override val source: SourceReference.UnitSlice,
                val address: DirectiveParameterValue<ULong>,
            ) : Location

            data class In(
                override val source: SourceReference.UnitSlice,
                val regionId: SourcedValue<NamespacedId>,
            ) : Location
        }
    }

    data class Bracket(
        val type: BracketType,
        val tokens: List<CompositorToken>,
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
