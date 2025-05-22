package net.voxelpi.axiom.asm.type

public sealed interface VariableLike : RegisterLike {

    public data class VariableName(
        val name: String,
    ) : VariableLike
}
