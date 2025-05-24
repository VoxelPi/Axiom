package net.voxelpi.axiom.asm.anchor

import java.util.UUID

public data class Label(
    override val uniqueId: UUID,
    override val name: String,
) : Anchor.Named
