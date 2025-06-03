package net.voxelpi.axiom.computer.state

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.register.Register

public data class ComputerStatePatch<R : ComputerStatePatch.Reason>(
    public val changes: List<ComputerStateChange>,
    public val reason: R,
) {

    public class Builder internal constructor(
        private val base: ComputerStateStorage,
    ) : MutableComputerState {
        private val changes = mutableListOf<ComputerStateChange>()

        override val architecture: Architecture
            get() = base.architecture

        private val stackMirror = base.stackState.copy()

        public fun <T : Reason> build(reason: T): ComputerStatePatch<T> {
            return ComputerStatePatch(changes.toList(), reason)
        }

        override fun register(register: Register): ULong {
            // Find the last change for the register.
            val change = changes
                .filterIsInstance<ComputerStateChange.RegisterChange>()
                .lastOrNull { it.register == register }

            // Return the new value of the latest change to the register, or null if the register was never changed.
            return change?.newValue ?: base.register(register)
        }

        override fun writeRegister(register: Register, value: ULong) {
            val previousValue = register(register)
            val newValue = register.type.unsignedValueOf(value)
            changes += ComputerStateChange.RegisterChange(register, previousValue, newValue)
        }

        override fun memoryCell(address: Int): ULong {
            // Find the last change for the memory address.
            val change = changes
                .filterIsInstance<ComputerStateChange.MemoryChange>()
                .lastOrNull { it.address == address }

            // Return the new value of the latest change to the memory address, or null if the memory address was never changed.
            return change?.newValue ?: base.memoryCell(address)
        }

        override fun writeMemoryCell(address: Int, value: ULong) {
            val previousValue = memoryCell(address)
            val newValue = base.architecture.memoryWordType.unsignedValueOf(value)
            changes += ComputerStateChange.MemoryChange(address, previousValue, newValue)
        }

        override fun stackPointer(): Int {
            return stackMirror.size
        }

        override fun stackCell(address: Int): ULong {
            return stackMirror[address]
        }

        override fun writeStackCell(address: Int, value: ULong) {
            val previousValue = stackMirror[address]
            val newValue = base.architecture.stackWordType.unsignedValueOf(value)
            stackMirror[address] = newValue
            changes += ComputerStateChange.Stack.Change(address, previousValue, newValue)
        }

        override fun writeStackPointer(address: Int) {
            val newAddress = address % base.architecture.stackSize
            val previousAddress = stackPointer()
            stackMirror.jump(newAddress)
            changes += ComputerStateChange.Stack.PointerChange(previousAddress, newAddress)
        }

        override fun stackPush(value: ULong) {
            val newValue = base.architecture.stackWordType.unsignedValueOf(value)
            changes += ComputerStateChange.Stack.Push(newValue)
            stackMirror.push(newValue)
        }

        override fun stackPop(): ULong {
            val value = stackMirror.pop()
            changes.add(ComputerStateChange.Stack.Pop(value))
            return value
        }

        override fun carry(): Boolean {
            // Find the last change for the register.
            val change = changes
                .filterIsInstance<ComputerStateChange.CarryChange>()
                .lastOrNull()

            // Return the new value of the latest change to the register, or null if the register was never changed.
            return change?.newValue ?: base.carry()
        }

        override fun writeCarry(value: Boolean) {
            changes += ComputerStateChange.CarryChange(carry(), value)
        }
    }

    public sealed interface Reason {

        public data object External : Reason

        public sealed interface InstructionExecution : Reason {
            public val instruction: Instruction
            public val valueA: ULong
            public val valueB: ULong
            public val output: ULong?
            public val valueConditionRegister: ULong
            public val conditionMet: Boolean
            public val hitBreak: Boolean

            public data class Program(
                val iInstruction: Int,
                override val instruction: Instruction,
                override val valueA: ULong,
                override val valueB: ULong,
                override val output: ULong?,
                override val valueConditionRegister: ULong,
                override val conditionMet: Boolean,
                override val hitBreak: Boolean,
            ) : InstructionExecution

            public data class Inline(
                override val instruction: Instruction,
                override val valueA: ULong,
                override val valueB: ULong,
                override val output: ULong?,
                override val valueConditionRegister: ULong,
                override val conditionMet: Boolean,
                override val hitBreak: Boolean,
            ) : InstructionExecution
        }
    }
}
