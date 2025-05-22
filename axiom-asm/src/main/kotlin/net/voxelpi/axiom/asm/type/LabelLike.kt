package net.voxelpi.axiom.asm.type

public sealed interface LabelLike {

    public data class LabelName(
        val name: String,
    ) : LabelLike
}
