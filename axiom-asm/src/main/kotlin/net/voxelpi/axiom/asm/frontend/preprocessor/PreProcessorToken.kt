package net.voxelpi.axiom.asm.frontend.preprocessor

import net.voxelpi.axiom.asm.frontend.token.Token
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.Sourced
import net.voxelpi.axiom.asm.source.SourcedValue

public sealed interface PreProcessorToken : Token {

    override val source: SourceReference.UnitSlice

    public sealed interface Separator : PreProcessorToken {

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
    ) : PreProcessorToken

    public data class Integer(
        public val value: Long,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class Text(
        public val value: String,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class Bracket(
        val type: BracketType,
        val tokens: List<PreProcessorToken>,
        val openingBracketSource: SourceReference.UnitSlice,
        val closingBracketSource: SourceReference.UnitSlice,
    ) : PreProcessorToken {

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

    public data class Scope(
        val tokens: List<PreProcessorToken>,
        val location: Location,
        val public: List<PreProcessorToken>,
        val openingBracketSource: SourceReference.UnitSlice,
        val closingBracketSource: SourceReference.UnitSlice,
    ) : PreProcessorToken {

        override val source: SourceReference.UnitSlice
            get() {
                require(openingBracketSource.unit == closingBracketSource.unit)
                return SourceReference.UnitSlice(
                    openingBracketSource.unit,
                    openingBracketSource.index,
                    closingBracketSource.index - openingBracketSource.index + 1,
                )
            }

        public sealed interface Location {
            public data object Inline : Location

            public data class Absolute(val location: PreProcessorToken) : Location

            public data class Region(val regionId: PreProcessorToken) : Location
        }
    }

    public interface TemplateDirective : PreProcessorToken {

        public val args: List<Argument>
        public val body: List<PreProcessorToken>

        public data class Anonymous(
            override val args: List<Argument>,
            override val body: List<PreProcessorToken>,
            override val source: SourceReference.UnitSlice,
        ) : TemplateDirective

        public data class Named(
            val id: SourcedValue<NamespacedId>,
            override val args: List<Argument>,
            override val body: List<PreProcessorToken>,
            override val source: SourceReference.UnitSlice,
        ) : TemplateDirective

        public sealed interface Argument : Sourced {

            public data class Value(
                val id: SourcedValue<NamespacedId>,
                val default: SourcedValue<List<PreProcessorToken>>,
            )

            public data class Template(
                val id: SourcedValue<NamespacedId>,
                val args: List<Argument>,
                val default: SourcedValue<List<PreProcessorToken>>,
            )
        }
    }

    public data class IncludeDirective(
        val path: SourcedValue<String>,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class InsertDirective(
        val templateId: SourcedValue<NamespacedId>,
        val args: List<PreProcessorToken>,
        val kwargs: Map<String, Pair<SourceReference, PreProcessorToken>>,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class AtDirective(
        val location: SourcedValue<Int>,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class InDirective(
        val regionId: SourcedValue<NamespacedId>,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class GlobalDirective(
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class PublicDirective(
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class DefineDirective(
        val id: SourcedValue<NamespacedId>,
        val value: List<PreProcessorToken>,
        override val source: SourceReference.UnitSlice,
    ) : PreProcessorToken

    public data class RegionDirective(
        val id: SourcedValue<NamespacedId>,
        val capacity: SourcedValue<Long>?,
    )
}
