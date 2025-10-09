package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.language.NamespacedId

internal data class CompositorTemplatePrototype(
    val positionArgs: List<List<CompositorToken>>,
    val keywordArgs: Map<NamespacedId, List<CompositorToken>>,
)
