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

    public data class StackPush(
        val value: ULong,
    ) : ComputerStateChange

    public data class StackPop(
        val value: ULong,
    ) : ComputerStateChange

    public data class CarryChange(
        val previousValue: Boolean,
        val newValue: Boolean,
    ) : ComputerStateChange
}
