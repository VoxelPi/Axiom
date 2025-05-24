package net.voxelpi.axiom.asm.variable

import net.voxelpi.axiom.asm.type.ValueLike

public data class Variable(
    public val name: String,
    public val value: ValueLike,
)
