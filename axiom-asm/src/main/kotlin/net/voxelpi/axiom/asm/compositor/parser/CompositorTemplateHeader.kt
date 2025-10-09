package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.language.NamespacedId

internal data class CompositorTemplateHeader(
    val parameters: List<Parameter>,
) {

    internal data class Parameter(
        val id: NamespacedId,
        val default: List<CompositorToken>?,
    )
}
