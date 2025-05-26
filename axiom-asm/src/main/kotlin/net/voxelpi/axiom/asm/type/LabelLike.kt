package net.voxelpi.axiom.asm.type

public sealed interface LabelLike : AnchorLike {

    public data class LabelName(
        val name: String,
    ) : LabelLike
}
