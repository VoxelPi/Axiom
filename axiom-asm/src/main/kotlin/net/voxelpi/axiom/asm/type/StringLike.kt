package net.voxelpi.axiom.asm.type

public sealed interface StringLike {

    public data class StringValue(
        public val value: String,
    ) : StringLike
}
