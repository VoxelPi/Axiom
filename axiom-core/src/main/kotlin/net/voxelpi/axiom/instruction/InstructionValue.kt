package net.voxelpi.axiom.instruction

import net.voxelpi.axiom.register.RegisterVariable

public sealed interface InstructionValue {

    public data class ImmediateValue(
        val value: Long,
    ) : InstructionValue {

        override fun toString(): String {
            return value.toString()
        }
    }

    public data class RegisterReference(
        val register: RegisterVariable,
    ) : InstructionValue {

        override fun toString(): String {
            return register.id
        }
    }
}
