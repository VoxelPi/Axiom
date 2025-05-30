package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.ComputerStack
import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterVariable

public class MutableComputerState<P : Comparable<P>>(
    public val architecture: Architecture<P, *>,
) : ComputerState<P> {

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

    override fun <R : Comparable<R>> registerState(register: Register<R>): R {
        return castToWordType(registerValues[register.id]!!, register.type)
    }

    override fun registerStateUInt64(register: Register<*>): ULong {
        return registerValues[register.id]!! and register.type.mask
    }

    override fun registerStateInt64(register: Register<*>): Long {
        var value = registerStateUInt64(register)
        val negative = value and (1UL shl (register.type.bits - 1)) != 0UL
        if (negative) {
            value = value or register.type.mask.inv()
        }
        return value.toLong()
    }

    public fun makeRegisterModification(register: Register<*>, value: ULong): ComputerStateChange.RegisterChange {
        val newValue = value and register.type.mask
        val previousValue = registerValues[register.id]!!
        registerValues[register.id] = newValue
        return ComputerStateChange.RegisterChange(register, previousValue, newValue)
    }

    override fun registerVariableStateUInt64(variable: RegisterVariable<*, *>): ULong {
        return when (variable) {
            is RegisterVariable.Direct<*> -> registerStateUInt64(variable.register)
            is RegisterVariable.Part<*, *> -> {
                val registerState = registerStateUInt64(variable.register)
                return (registerState shr (variable.part * variable.type.bits)) and variable.type.mask
            }
        }
    }

    override fun registerVariableStateInt64(variable: RegisterVariable<*, *>): Long {
        var value = registerVariableStateUInt64(variable)
        val negative = value and (1UL shl (variable.type.bits - 1)) != 0UL
        if (negative) {
            value = value or variable.type.mask.inv()
        }
        return value.toLong()
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

    public fun makeRegisterVariableModification(variable: RegisterVariable<*, *>, value: ULong): ComputerStateChange.RegisterChange {
        when (variable) {
            is RegisterVariable.Direct<*> -> {
                return makeRegisterModification(variable.register, value)
            }
            is RegisterVariable.Part<*, *> -> {
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

    public fun memoryCellState(address: Int): ULong {
        return memoryState[address]
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

    public fun redoPatch(patch: ComputerStatePatch) {
        for (change in patch.changes) {
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

    public fun undoPatch(patch: ComputerStatePatch) {
        for (change in patch.changes) {
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
