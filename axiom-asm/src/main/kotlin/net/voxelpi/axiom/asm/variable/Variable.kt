package net.voxelpi.axiom.asm.variable

import net.voxelpi.axiom.asm.type.ValueLike
import java.util.UUID

public data class Variable(
    public val uniqueId: UUID,
    public val name: String,
    public val value: ValueLike,
)
