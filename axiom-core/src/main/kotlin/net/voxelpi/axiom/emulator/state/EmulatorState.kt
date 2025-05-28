package net.voxelpi.axiom.emulator.state

import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public interface EmulatorState {

    public fun <R : Comparable<R>> registerState(register: Register<R>): R

    public fun <R : Comparable<R>, V : Comparable<V>> registerVariableState(variable: RegisterVariable<V, R>): V
}
