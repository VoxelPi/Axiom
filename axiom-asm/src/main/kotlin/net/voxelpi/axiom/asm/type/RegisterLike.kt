package net.voxelpi.axiom.asm.type

public sealed interface RegisterLike : ValueLike {

    public data class UnparsedRegister(
        public val value: String,
    ) : RegisterLike
}
