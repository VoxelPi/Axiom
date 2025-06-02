package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public interface MutableComputerState : ComputerState {

    public fun writeRegister(register: Register, value: ULong)

    public fun writeRegisterVariable(variable: RegisterVariable, value: ULong) {
        when (variable) {
            is RegisterVariable.Direct -> writeRegister(variable.register, value)
            is RegisterVariable.Part -> {
                val registerValue = register(variable.register)
                writeRegister(variable.register, (registerValue and variable.type.mask.inv()) or ((value and variable.type.mask) shl (variable.part * variable.type.bits)))
            }
        }
    }

    public fun writeMemoryCell(address: Int, value: ULong)

    public fun writeModifyStackCell(address: Int, value: ULong)

    public fun writeStackPointer(address: Int)

    public fun pushStack(value: ULong)

    public fun popStack(): ULong

    public fun writeCarry(value: Boolean)
}
