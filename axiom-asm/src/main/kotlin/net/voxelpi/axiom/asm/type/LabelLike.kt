package net.voxelpi.axiom.asm.type

public sealed interface LabelLike : ValueLike {

    public data class LabelName(
        val name: String,
    ) : LabelLike
}
