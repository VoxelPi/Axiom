package net.voxelpi.axiom.asm.type

public sealed interface ScopeLike : AnchorLike {

    public data class ScopeName(
        public val name: String,
    ) : ScopeLike
}
