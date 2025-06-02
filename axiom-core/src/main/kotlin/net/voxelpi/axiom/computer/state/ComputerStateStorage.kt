package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.ComputerStack
import net.voxelpi.axiom.register.Register

public class ComputerStateStorage(
    override val architecture: Architecture,
) : ComputerState {

    public var carryState: Boolean = false

    public val registerValues: MutableMap<String, ULong> = architecture.registers.registers().associate {
        it.id to 0UL
    }.toMutableMap()

    internal val memoryState: ULongArray = ULongArray(architecture.memorySize) { 0UL }
    internal val stackState: ComputerStack = ComputerStack(architecture.stackSize)

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

    override fun register(register: Register): ULong {
        return register.type.unsignedValueOf(registerValues[register.id]!!)
    }

    override fun memoryCell(address: Int): ULong {
        return memoryState[address]
    }

    override fun stackPointer(): Int {
        return stackState.size
    }

    override fun stackCell(address: Int): ULong {
        return stackState[address]
    }

    override fun stackPeek(): ULong {
        return stackState.peek()
    }

    override fun carry(): Boolean {
        return carryState
    }

    public fun <T : ComputerStatePatch.Reason> modify(block: ComputerStatePatch.Builder.() -> T): ComputerStatePatch<T> {
        val builder = ComputerStatePatch.Builder(this)
        val reason = builder.block()
        val patch = builder.build(reason)
        redoPatch(patch)
        return patch
    }

    public fun redoPatch(patch: ComputerStatePatch<*>) {
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
                is ComputerStateChange.Stack.Pop -> {
                    stackState.pop()
                }
                is ComputerStateChange.Stack.Push -> {
                    stackState.push(change.value)
                }
                is ComputerStateChange.Stack.Change -> {
                    stackState[change.address] = change.newValue
                }
                is ComputerStateChange.Stack.PointerChange -> {
                    stackState.jump(change.newAddress)
                }
            }
        }
    }

    public fun undoChanges(patch: ComputerStatePatch<*>) {
        for (change in patch.changes.reversed()) {
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
                is ComputerStateChange.Stack.Pop -> {
                    stackState.push(change.value)
                }
                is ComputerStateChange.Stack.Push -> {
                    stackState.pop()
                }
                is ComputerStateChange.Stack.Change -> {
                    stackState[change.address] = change.previousValue
                }
                is ComputerStateChange.Stack.PointerChange -> {
                    stackState.jump(change.previousAddress)
                }
            }
        }
    }
}
