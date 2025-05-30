package net.voxelpi.axiom.emulator.state

import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public interface EmulatorState<P : Comparable<P>> {

    public fun <R : Comparable<R>> registerState(register: Register<R>): R

    public fun registerStateInt64(register: Register<*>): Long

    public fun registerStateUInt64(register: Register<*>): ULong

    public fun <R : Comparable<R>, V : Comparable<V>> registerVariableState(variable: RegisterVariable<V, R>): V

    public fun registerVariableStateInt64(variable: RegisterVariable<*, *>): Long

    public fun registerVariableStateUInt64(variable: RegisterVariable<*, *>): ULong
}
