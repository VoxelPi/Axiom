package net.voxelpi.axiom.asm.type

import net.voxelpi.axiom.asm.anchor.Anchor

public sealed interface AnchorLike : ValueLike {

    public data class AnchorReference(
        public val anchor: Anchor,
    ) : AnchorLike
}
