package net.voxelpi.axiom.emulator.state

import net.voxelpi.axiom.register.Register

public sealed interface EmulatorStateChange {

    public data class RegisterChange(
        val register: Register<*>,
        val previousValue: ULong,
        val newValue: ULong,
    ) : EmulatorStateChange

    public data class MemoryChange(
        val address: Int,
        val previousValue: ULong,
        val newValue: ULong,
    ) : EmulatorStateChange

    public data class StackPush(
        val value: ULong,
    ) : EmulatorStateChange

    public data class StackPop(
        val value: ULong,
    ) : EmulatorStateChange

    public data class CarryChange(
        val previousValue: Boolean,
        val newValue: Boolean,
    ) : EmulatorStateChange
}
