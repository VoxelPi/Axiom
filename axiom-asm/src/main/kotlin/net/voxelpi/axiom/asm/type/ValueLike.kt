package net.voxelpi.axiom.asm.type

public sealed interface ValueLike {

    public data class UnparsedValue(
        public val value: String,
    ) : ValueLike
}
