package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public interface ComputerState {

    public fun registerState(register: Register): ULong

    public fun registerStateInt64(register: Register): Long

    public fun registerStateUInt64(register: Register): ULong

    public fun registerVariableState(variable: RegisterVariable): ULong

    public fun registerVariableStateInt64(variable: RegisterVariable): Long

    public fun registerVariableStateUInt64(variable: RegisterVariable): ULong

    public fun memoryState(address: Int): ULong

    public fun stackPointerState(): Int

    public fun stackState(address: Int): ULong?

    public fun stackTopState(): ULong
}
