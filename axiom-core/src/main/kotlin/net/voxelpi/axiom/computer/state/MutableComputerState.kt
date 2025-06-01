package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.ComputerStack
import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public class MutableComputerState(
    public val architecture: Architecture,
) : ComputerState {

    public var carryState: Boolean = false

    public val registerValues: MutableMap<String, ULong> = architecture.registers.registers().associate {
        it.id to 0UL
    }.toMutableMap()

    public val memoryState: ULongArray = ULongArray(architecture.memorySize) { 0UL }
    public val stackState: ComputerStack = ComputerStack(architecture.stackSize)

    public fun clear() {
        carryState = false
        registerValues.putAll(
            architecture.registers.registers().associate {
                it.id to 0UL
            }
        )
        for (i in memoryState.indices) {
            memoryState[i] = 0UL
        }
        stackState.clear()
    }

    override fun registerState(register: Register): ULong {
        return register.type.unsignedValueOf(registerValues[register.id]!!)
    }

    override fun registerStateUInt64(register: Register): ULong {
        return register.type.unsignedValueOf(registerValues[register.id]!!)
    }

    override fun registerStateInt64(register: Register): Long {
        return register.type.signedValueOf(registerValues[register.id]!!)
    }

    public fun makeRegisterModification(register: Register, value: ULong): ComputerStateChange.RegisterChange {
        val newValue = register.type.unsignedValueOf(value)
        val previousValue = registerValues[register.id]!!
        registerValues[register.id] = newValue
        return ComputerStateChange.RegisterChange(register, previousValue, newValue)
    }

    override fun registerVariableState(variable: RegisterVariable): ULong {
        return when (variable) {
            is RegisterVariable.Direct -> registerState(variable.register)
            is RegisterVariable.Part -> {
                val registerState = registerState(variable.register)
                return (registerState shr (variable.part * variable.type.bits)) and variable.type.mask
            }
        }
    }

    override fun registerVariableStateUInt64(variable: RegisterVariable): ULong {
        return registerVariableState(variable)
    }

    override fun registerVariableStateInt64(variable: RegisterVariable): Long {
        val value = registerVariableState(variable)
        return variable.type.signedValueOf(value)
    }

    public fun makeRegisterVariableModification(variable: RegisterVariable, value: ULong): ComputerStateChange.RegisterChange {
        when (variable) {
            is RegisterVariable.Direct -> {
                return makeRegisterModification(variable.register, value)
            }
            is RegisterVariable.Part -> {
                val partMask = variable.type.mask shl (variable.part * variable.type.bits)

                val previousValue = registerValues[variable.register.id]!!
                val newValue = (value and partMask) or (previousValue and partMask.inv())
                registerValues[variable.register.id] = newValue
                return ComputerStateChange.RegisterChange(variable.register, previousValue, newValue)
            }
        }
    }

    public fun makeCarryModification(value: Boolean): ComputerStateChange.CarryChange {
        val previousValue = carryState
        carryState = value
        return ComputerStateChange.CarryChange(previousValue, value)
    }

    override fun memoryState(address: Int): ULong {
        return memoryState[address]
    }

    override fun stackPointerState(): Int {
        return stackState.size
    }

    override fun stackState(address: Int): ULong? {
        return stackState[address]
    }

    override fun stackTopState(): ULong {
        return stackState.peek()
    }

    public fun makeMemoryModification(address: Int, value: ULong): ComputerStateChange.MemoryChange {
        val newValue = value and architecture.memoryWordType.mask
        val previousValue = memoryState[address]
        memoryState[address] = newValue
        return ComputerStateChange.MemoryChange(address, previousValue, newValue)
    }

    public fun stackPeek(): ULong {
        return stackState.peek()
    }

    public fun makeStackPush(value: ULong): ComputerStateChange.StackPush {
        val newValue = value and architecture.stackWordType.mask
        stackState.push(newValue)
        return ComputerStateChange.StackPush(newValue)
    }

    public fun makeStackPop(): Pair<ComputerStateChange.StackPop, ULong> {
        val value = stackState.pop()
        return Pair(ComputerStateChange.StackPop(value), value)
    }

    public fun redoChanges(changes: Collection<ComputerStateChange>) {
        for (change in changes) {
            when (change) {
                is ComputerStateChange.CarryChange -> {
                    carryState = change.newValue
                }
                is ComputerStateChange.MemoryChange -> {
                    memoryState[change.address] = change.newValue
                }
                is ComputerStateChange.RegisterChange -> {
                    registerValues[change.register.id] = change.newValue
                }
                is ComputerStateChange.StackPop -> {
                    stackState.pop()
                }
                is ComputerStateChange.StackPush -> {
                    stackState.push(change.value)
                }
            }
        }
    }

    public fun undoChanges(changes: Collection<ComputerStateChange>) {
        for (change in changes) {
            when (change) {
                is ComputerStateChange.CarryChange -> {
                    carryState = change.previousValue
                }
                is ComputerStateChange.MemoryChange -> {
                    memoryState[change.address] = change.previousValue
                }
                is ComputerStateChange.RegisterChange -> {
                    registerValues[change.register.id] = change.previousValue
                }
                is ComputerStateChange.StackPop -> {
                    stackState.push(change.value)
                }
                is ComputerStateChange.StackPush -> {
                    stackState.pop()
                }
            }
        }
    }
}
