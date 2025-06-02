package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public interface ComputerState {

    public val architecture: Architecture

    public fun register(register: Register): ULong

    public fun registerVariable(variable: RegisterVariable): ULong {
        return when (variable) {
            is RegisterVariable.Direct -> register(variable.register)
            is RegisterVariable.Part -> {
                val registerState = register(variable.register)
                return (registerState shr (variable.part * variable.type.bits)) and variable.type.mask
            }
        }
    }

    public fun memoryCell(address: Int): ULong

    public fun stackPointer(): Int

    public fun stackCell(address: Int): ULong

    public fun stackPeek(): ULong {
        return stackCell(stackPointer())
    }

    public fun carry(): Boolean
}
