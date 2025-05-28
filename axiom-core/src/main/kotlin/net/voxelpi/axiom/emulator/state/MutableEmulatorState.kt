package net.voxelpi.axiom.emulator.state

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public class MutableEmulatorState<P : Comparable<P>>(
    public val architecture: Architecture<P, *>,
) : EmulatorState {

    public var carryState: Boolean = false

    public val registerValues: MutableMap<String, ULong> = architecture.registers.registers().associate {
        it.id to 0UL
    }.toMutableMap()

    public val memoryState: ULongArray = ULongArray(architecture.memorySize) { 0UL }
    public val stackState: ULongArray = ULongArray(architecture.stackSize) { 0UL }
    public var stackPointer: Int = 0

    override fun <R : Comparable<R>> registerState(register: Register<R>): R {
        return castToWordType(registerValues[register.id]!!, register.type)
    }

    public fun rawRegisterState(register: Register<*>): ULong {
        return registerValues[register.id]!!
    }

    public fun makeRegisterModification(register: Register<*>, value: ULong): EmulatorStateChange.RegisterChange {
        val newValue = value and register.type.mask
        val previousValue = registerValues[register.id]!!
        registerValues[register.id] = newValue
        return EmulatorStateChange.RegisterChange(register, previousValue, newValue)
    }

    public fun rawRegisterVariableState(variable: RegisterVariable<*, *>): ULong {
        return when (variable) {
            is RegisterVariable.Direct<*> -> rawRegisterState(variable.register)
            is RegisterVariable.Part<*, *> -> {
                val registerState = rawRegisterState(variable.register)
                return (registerState shr (variable.part * variable.type.bits)) and variable.type.mask
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Comparable<R>, V : Comparable<V>> registerVariableState(variable: RegisterVariable<V, R>): V {
        when (variable) {
            is RegisterVariable.Direct<*> -> {
                return registerState(variable.register) as V
            }
            is RegisterVariable.Part<*, *> -> {
                val registerState = registerValues[variable.register.id]!!
                val value = (registerState shr (variable.part * variable.type.bits)) and variable.type.mask
                return castToWordType(value, variable.type)
            }
        }
    }

    public fun makeRegisterVariableModification(variable: RegisterVariable<*, *>, value: ULong): EmulatorStateChange.RegisterChange {
        when (variable) {
            is RegisterVariable.Direct<*> -> {
                return makeRegisterModification(variable.register, value)
            }
            is RegisterVariable.Part<*, *> -> {
                val partMask = variable.type.mask shl (variable.part * variable.type.bits)

                val previousValue = registerValues[variable.register.id]!!
                val newValue = (value and partMask) or (previousValue and partMask.inv())
                registerValues[variable.register.id] = newValue
                return EmulatorStateChange.RegisterChange(variable.register, previousValue, newValue)
            }
        }
    }

    public fun makeCarryModification(value: Boolean): EmulatorStateChange.CarryChange {
        val previousValue = carryState
        carryState = value
        return EmulatorStateChange.CarryChange(previousValue, value)
    }

    public fun memoryCellState(address: Int): ULong {
        return memoryState[address]
    }

    public fun makeMemoryModification(address: Int, value: ULong): EmulatorStateChange.MemoryChange {
        val previousValue = memoryState[address]
        memoryState[address] = value
        return EmulatorStateChange.MemoryChange(address, previousValue, value)
    }

    public fun stackPeek(): ULong {
        return stackState[if (stackPointer == 0) stackState.size - 1 else stackPointer - 1]
    }

    public fun makeStackPush(value: ULong): EmulatorStateChange.StackPush {
        stackState[stackPointer] = value
        stackPointer++
        if (stackPointer >= stackState.size) {
            stackPointer = 0
        }
        return EmulatorStateChange.StackPush(value)
    }

    public fun makeStackPop(): Pair<EmulatorStateChange.StackPop, ULong> {
        stackPointer--
        if (stackPointer < 0) {
            stackPointer = stackState.size - 1
        }
        val value = stackState[stackPointer]
        return Pair(EmulatorStateChange.StackPop(value), value)
    }

    public fun redoPatch(patch: EmulatorStatePatch) {
        for (change in patch.changes) {
            when (change) {
                is EmulatorStateChange.CarryChange -> {
                    carryState = change.newValue
                }
                is EmulatorStateChange.MemoryChange -> {
                    memoryState[change.address] = change.newValue
                }
                is EmulatorStateChange.RegisterChange -> {
                    registerValues[change.register.id] = change.newValue
                }
                is EmulatorStateChange.StackPop -> {
                    stackPointer--
                    if (stackPointer < 0) {
                        stackPointer = stackState.size - 1
                    }
                }
                is EmulatorStateChange.StackPush -> {
                    stackState[stackPointer] = change.value
                    stackPointer++
                    if (stackPointer >= stackState.size) {
                        stackPointer = 0
                    }
                }
            }
        }
    }

    public fun undoPatch(patch: EmulatorStatePatch) {
        for (change in patch.changes) {
            when (change) {
                is EmulatorStateChange.CarryChange -> {
                    carryState = change.previousValue
                }
                is EmulatorStateChange.MemoryChange -> {
                    memoryState[change.address] = change.previousValue
                }
                is EmulatorStateChange.RegisterChange -> {
                    registerValues[change.register.id] = change.previousValue
                }
                is EmulatorStateChange.StackPop -> {
                    stackState[stackPointer] = change.value
                    stackPointer++
                    if (stackPointer >= stackState.size) {
                        stackPointer = 0
                    }
                }
                is EmulatorStateChange.StackPush -> {
                    stackPointer--
                    if (stackPointer < 0) {
                        stackPointer = stackState.size - 1
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> castToWordType(value: ULong, wordType: WordType<T>): T {
        return when (wordType) {
            WordType.INT8 -> value.toUByte() as T
            WordType.INT16 -> value.toUShort() as T
            WordType.INT32 -> value.toUInt() as T
            WordType.INT64 -> value as T
        }
    }
}
