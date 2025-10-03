package net.voxelpi.axiom.asm.compositor.token

import net.voxelpi.axiom.asm.source.SourceReference

internal data class CompositorScope(
    val tokens: List<CompositorToken>,
    val location: Location,
    val public: List<CompositorToken>,
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

    sealed interface Location {
        data object Inline : Location

        data class Absolute(val location: CompositorToken) : Location

        data class Region(val regionId: CompositorToken) : Location
    }
}
