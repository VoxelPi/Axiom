package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.register.Register

public sealed interface ComputerStateChange {

    public data class RegisterChange(
        val register: Register,
        val previousValue: ULong,
        val newValue: ULong,
    ) : ComputerStateChange

    public data class MemoryChange(
        val address: Int,
        val previousValue: ULong,
        val newValue: ULong,
    ) : ComputerStateChange

    public sealed interface Stack : ComputerStateChange {

        public data class PointerChange(
            val previousAddress: Int,
            val newAddress: Int,
        ) : Stack

        public data class Change(
            val address: Int,
            val previousValue: ULong,
            val newValue: ULong,
        ) : Stack

        public data class Push(
            val value: ULong,
        ) : Stack

        public data class Pop(
            val value: ULong,
        ) : Stack
    }

    public data class CarryChange(
        val previousValue: Boolean,
        val newValue: Boolean,
    ) : ComputerStateChange
}
