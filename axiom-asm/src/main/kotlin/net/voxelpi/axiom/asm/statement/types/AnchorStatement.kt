package net.voxelpi.axiom.asm.statement.types

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.statement.annotation.StatementType

@StatementType("anchor")
public data class AnchorStatement(
    val anchor: Anchor,
)
