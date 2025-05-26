package net.voxelpi.axiom.asm.type

import net.voxelpi.axiom.Register

public sealed interface RegisterLike : ValueLike {

    public data class RegisterName(
        public val name: String,
    ) : RegisterLike

    public data object PC : RegisterLike

    public data class RegisterReference(
        val register: Register,
    ) : RegisterLike
}
