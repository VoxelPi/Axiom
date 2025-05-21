package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.source.SourceLink

public data class Label(
    override val source: SourceLink,
    override val name: String,
) : Anchor.Named
