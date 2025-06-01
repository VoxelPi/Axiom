package net.voxelpi.axiom.asm.type

import net.voxelpi.axiom.register.RegisterVariable

public sealed interface RegisterLike : ValueLike {

    public data class RegisterName(
        public val name: String,
    ) : RegisterLike

    public data class AnyRegister(
        public val readable: Boolean = false,
        public val writable: Boolean = false,
        public val conditionable: Boolean = false,
    ) : RegisterLike

    public data object PC : RegisterLike

    public data class RegisterReference(
        val register: RegisterVariable,
    ) : RegisterLike
}
