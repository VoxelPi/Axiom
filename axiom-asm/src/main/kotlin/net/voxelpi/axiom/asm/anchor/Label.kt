package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.source.SourceLink
import java.util.UUID

public data class Label(
    override val source: SourceLink,
    override val uniqueId: UUID,
    override val name: String,
) : Anchor.Named
