package net.voxelpi.axiom.computer

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.arch.MemoryMap
import net.voxelpi.axiom.computer.state.ComputerState
import net.voxelpi.axiom.computer.state.ComputerStatePatch
import net.voxelpi.axiom.computer.state.ComputerStateStorage
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.instruction.ProgramConstant
import net.voxelpi.axiom.instruction.ProgramElement
import net.voxelpi.axiom.register.RegisterVariable
import kotlin.math.sqrt

public class Computer(
    public val architecture: Architecture,
    public val inputAvailableProvider: () -> Boolean,
    public val inputProvider: () -> ULong,
    public val outputHandler: (ULong) -> Unit,
    public val warningHandler: (String) -> Unit,
) {

    public var program: Program = Program(emptyList())
        private set

    private val state: ComputerStateStorage = ComputerStateStorage(architecture)

    private var iStep = 0
    private val patches: MutableList<ComputerStatePatch<*>> = mutableListOf()

    public fun loadProgram(program: Program) {
        this.program = program
        reset()
    }

    public fun reset() {
        state.clear()
        iStep = 0
        patches.clear()
    }

    public fun currentState(): ComputerState {
        return state
    }

    public fun modifyState(block: ComputerStatePatch.Builder.() -> Unit): ComputerState {
        val patch = state.modify {
            block()
            ComputerStatePatch.Reason.External
        }
        applyPatch(patch)
        return state
    }

    public fun runUntilBreak(): Int {
        var nExecutedInstructions = 0
        while (true) {
            val patch = runSingleInstruction()
            ++nExecutedInstructions
            if (patch.reason.hitBreak) {
                break
            }
        }
        return nExecutedInstructions
    }

    public fun runSingleInstruction(): ComputerStatePatch<ComputerStatePatch.Reason.InstructionExecution.Program> {
        val patch = state.modify {
            var hitBreak = false

            // FETCH
            val instructionIndex = state.register(architecture.registers.programCounter)
            val instruction = if (instructionIndex.toInt() in program.data.indices) {
                val element = program.data[instructionIndex.toInt()]
                if (element !is Instruction) {
                    throw Exception("Program at index $instructionIndex is not an instruction")
                }
                element
            } else {
                warningHandler("PC is outside of program")
                hitBreak = true
                val operation = if (architecture.registers.programCounterVariable.needsMode2 || architecture.dataWordType < architecture.registers.programCounterVariable.type) {
                    Operation.LOAD_2
                } else {
                    Operation.LOAD
                }
                Instruction(
                    operation,
                    Condition.ALWAYS,
                    architecture.registers.variables.values.firstOrNull { it.conditionable }!!,
                    architecture.registers.programCounterVariable,
                    InstructionValue.ImmediateValue(0),
                    InstructionValue.ImmediateValue(0),
                    emptyMap(),
                )
            }

            // DECODE
            val operation = instruction.operation
            val condition = instruction.condition
            val a: ULong = when (val value = instruction.inputA) {
                is InstructionValue.ImmediateValue -> value.value.toULong()
                is InstructionValue.RegisterReference -> state.registerVariable(value.register)
            }
            val b: ULong = when (val value = instruction.inputB) {
                is InstructionValue.ImmediateValue -> value.value.toULong()
                is InstructionValue.RegisterReference -> state.registerVariable(value.register)
            }
            val c: ULong = state.registerVariable(instruction.conditionRegister)
            val cSize = instruction.conditionRegister.type

            val outputRegister = instruction.outputRegister

            val carryIn = state.carryState

            // CONDITION
            val conditionValid: Boolean = evaluateCondition(condition, c, cSize)

            // EXECUTE
            var result: ULong? = null
            if (conditionValid) {
                val (executionResult, executionBreak) = executeOperation(operation, a, b, outputRegister, carryIn)
                if (executionResult != null) {
                    result = executionResult
                }
                if (executionBreak) {
                    hitBreak = true
                }
            }
            if (result != null && outputRegister.mask != null) {
                result = result and outputRegister.mask!!
            }

            // INCREMENT
            if (!conditionValid || result == null || outputRegister.register.id != architecture.registers.programCounter.id) {
                val nextProgramCounter = (instructionIndex + 1UL) and architecture.registers.programCounter.type.mask
                writeRegister(architecture.registers.programCounter, nextProgramCounter)
            }

            // STORE
            if (conditionValid && result != null) {
                writeRegisterVariable(outputRegister, result)
            }

            // Create the execution result.
            ComputerStatePatch.Reason.InstructionExecution.Program(
                instructionIndex.toInt(),
                instruction,
                a,
                b,
                result,
                c,
                conditionMet = conditionValid,
                hitBreak = hitBreak,
            )
        }
        applyPatch(patch)

        // Create and return the patch.
        return patch
    }

    public fun runInlineInstruction(
        instruction: Instruction,
    ): ComputerStatePatch<ComputerStatePatch.Reason.InstructionExecution.Inline> {
        var hitBreak = false

        val patch = state.modify {
            // DECODE
            val operation = instruction.operation
            val condition = instruction.condition
            val a: ULong = when (val value = instruction.inputA) {
                is InstructionValue.ImmediateValue -> value.value.toULong()
                is InstructionValue.RegisterReference -> state.registerVariable(value.register)
            }
            val b: ULong = when (val value = instruction.inputB) {
                is InstructionValue.ImmediateValue -> value.value.toULong()
                is InstructionValue.RegisterReference -> state.registerVariable(value.register)
            }
            val c: ULong = state.registerVariable(instruction.conditionRegister)
            val cSize = instruction.conditionRegister.type

            val outputRegister = instruction.outputRegister

            val carryIn = state.carryState

            // CONDITION
            val conditionValid: Boolean = evaluateCondition(condition, c, cSize)

            // EXECUTE
            var result: ULong? = null
            if (conditionValid) {
                val (executionResult, executionBreak) = executeOperation(operation, a, b, outputRegister, carryIn)
                if (executionResult != null) {
                    result = executionResult
                }
                if (executionBreak) {
                    hitBreak = true
                }
            }
            if (result != null && outputRegister.mask != null) {
                result = result and outputRegister.mask!!
            }

            // STORE
            if (conditionValid && result != null) {
                writeRegisterVariable(outputRegister, result)
            }

            // Create the execution result.
            ComputerStatePatch.Reason.InstructionExecution.Inline(
                instruction,
                a,
                b,
                result,
                c,
                conditionMet = conditionValid,
                hitBreak = hitBreak,
            )
        }
        applyPatch(patch)

        // Create and return the patch.
        return patch
    }

    private fun evaluateCondition(condition: Condition, c: ULong, cSize: WordType): Boolean {
        return when (condition) {
            Condition.ALWAYS -> true
            Condition.NEVER -> false
            Condition.EQUAL -> c == 0UL
            Condition.NOT_EQUAL -> c != 0UL
            Condition.LESS -> ((c shr (cSize.bits - 1)) and 1UL) != 0UL
            Condition.LESS_OR_EQUAL -> c == 0UL || ((c shr (cSize.bits - 1)) and 1UL) != 0UL
            Condition.GREATER -> c != 0UL && ((c shr (cSize.bits - 1)) and 1UL) == 0UL
            Condition.GREATER_OR_EQUAL -> ((c shr (cSize.bits - 1)) and 1UL) == 0UL
        }
    }

    private fun ComputerStatePatch.Builder.executeOperation(
        operation: Operation,
        a: ULong,
        b: ULong,
        outputRegister: RegisterVariable,
        carryIn: Boolean,
    ): Pair<ULong?, Boolean> {
        var result: ULong? = null
        var hitBreak = false

        when (operation) {
            Operation.CLEAR -> {
                result = 0UL
            }
            Operation.LOAD -> {
                result = a
            }
            Operation.LOAD_2 -> {
                val combinedValue = (a and architecture.dataWordType.mask) or ((b and architecture.dataWordType.mask) shl architecture.dataWordType.bits)
                result = combinedValue
            }
            Operation.AND -> {
                result = a and b
            }
            Operation.NAND -> {
                result = (a and b).inv() and outputRegister.type.mask
            }
            Operation.OR -> {
                result = a or b
            }
            Operation.NOR -> {
                result = (a or b).inv() and outputRegister.type.mask
            }
            Operation.XOR -> {
                result = a xor b
            }
            Operation.XNOR -> {
                result = (a xor b).inv() and outputRegister.type.mask
            }
            Operation.ADD -> {
                result = (a + b) and outputRegister.type.mask
                writeCarry(result < a || result < b)
            }
            Operation.SUBTRACT -> {
                val bInput = (b.inv() + 1UL) and architecture.dataWordType.mask
                result = (a + bInput) and outputRegister.type.mask
                writeCarry(result < a || result < bInput)
            }
            Operation.ADD_WITH_CARRY -> {
                // A + B + Cin
                val carryState = if (carryIn) 1UL else 0UL
                val partialSum = (a + b) and outputRegister.type.mask
                result = (a + b + carryState) and outputRegister.type.mask
                writeCarry(result < a || result < b || result < partialSum)
            }
            Operation.SUBTRACT_WITH_CARRY -> {
                // A - B - (1 - C_in) = A - (B + 1 - C_in) = A + (inv(B) + 1) + inv(C_in))
                val bInput = (b.inv() + 1UL) and architecture.dataWordType.mask
                val carryState = if (carryIn) 0UL else 1UL // INVERTED!!!

                val partialSum = (a + bInput) and outputRegister.type.mask
                result = (a + bInput + carryState) and outputRegister.type.mask
                writeCarry(result < a || result < bInput || result < partialSum)
            }
            Operation.INCREMENT -> {
                writeCarry(a == outputRegister.type.mask)
                result = (a + 1UL) and outputRegister.type.mask
            }
            Operation.DECREMENT -> {
                writeCarry(a != 0UL)
                result = (a - 1UL) and outputRegister.type.mask
            }
            Operation.MULTIPLY -> {
                result = (a * b) and outputRegister.type.mask
            }
            Operation.DIVIDE -> {
                result = if (b == 0UL) {
                    0UL
                } else {
                    a / b
                }
            }
            Operation.MODULO -> {
                result = if (b == 0UL) {
                    a and outputRegister.type.mask
                } else {
                    a % b
                }
            }
            Operation.SQRT -> {
                result = sqrt(a.toDouble()).toULong() and outputRegister.type.mask
            }
            Operation.SHIFT_LEFT -> {
                result = (a shl 1) and outputRegister.type.mask
                writeCarry((a shr (architecture.dataWordType.bits - 1)) and 1UL != 0UL)
            }
            Operation.SHIFT_RIGHT -> {
                result = (a shr 1) and outputRegister.type.mask
                writeCarry(((a shr 0) and 1UL) != 0UL)
            }
            Operation.ROTATE_LEFT -> {
                val carryState = if (carryIn) 1UL else 0UL
                result = ((a shl 1) or (carryState shl 0)) and outputRegister.type.mask
                writeCarry((a shr (architecture.dataWordType.bits - 1)) and 1UL != 0UL)
            }
            Operation.ROTATE_RIGHT -> {
                val carryState = if (carryIn) 1UL else 0UL
                result = ((a shr 1) or (carryState shl (architecture.dataWordType.bits - 1))) and outputRegister.type.mask
                writeCarry(((a shr 0) and 1UL) != 0UL)
            }
            Operation.BIT_GET -> {
                val bitValue = (1UL shl (b.toInt() and 0xFF)) and outputRegister.type.mask
                result = if ((a and bitValue) != 0UL) 1UL else 0UL
            }
            Operation.BIT_SET -> {
                val bitValue = (1UL shl (b.toInt() and 0xFF)) and outputRegister.type.mask
                result = a or bitValue
            }
            Operation.BIT_CLEAR -> {
                val bitValue = (1UL shl (b.toInt() and 0xFF)) and outputRegister.type.mask
                result = a and bitValue.inv()
            }
            Operation.BIT_TOGGLE -> {
                val bitValue = (1UL shl (b.toInt() and 0xFF)) and outputRegister.type.mask
                result = a xor bitValue
            }
            Operation.MEMORY_LOAD -> {
                val base = architecture.registers.indexRegister?.let { state.register(it) } ?: 0UL
                val offset = a
                val address = (base + offset).toInt()

                val mapping = architecture.memoryMap.selectMapping(address)
                when (mapping) {
                    is MemoryMap.MemoryMapping.Memory -> {
                        val value = state.memoryCell(mapping.map(address))
                        result = outputRegister.type.unsignedValueOf(value)
                    }
                    is MemoryMap.MemoryMapping.Program -> {
                        val programElement = program.data[mapping.map(address)]
                        val value = when (programElement) {
                            is Instruction -> architecture.dataWordType.unpackFirst(architecture.encodeInstruction(programElement).getOrThrow())
                            is ProgramConstant -> architecture.dataWordType.unsignedValueOf(programElement.value)
                            is ProgramElement.None -> {
                                warningHandler.invoke("Reading unused program memory.")
                                0UL
                            }
                        }
                        result = outputRegister.type.unsignedValueOf(value)
                    }
                    null -> {
                        warningHandler("Reading unmapped memory address $address")
                        result = 0UL
                    }
                }
            }
            Operation.MEMORY_STORE -> {
                val base = architecture.registers.indexRegister?.let { state.register(it) } ?: 0UL
                val offset = a
                val address = (base + offset).toInt()

                val mapping = architecture.memoryMap.selectMapping(address)
                when (mapping) {
                    is MemoryMap.MemoryMapping.Memory -> {
                        writeMemoryCell(mapping.map(address), b)
                    }
                    is MemoryMap.MemoryMapping.Program -> {
                        warningHandler("Writing to read-only memory address $address")
                    }
                    null -> {
                        warningHandler("Writing to unmapped memory address $address")
                    }
                }
            }
            Operation.IO_POLL -> {
                result = 1UL
            }
            Operation.IO_READ -> {
                result = inputProvider() and outputRegister.type.mask
            }
            Operation.IO_WRITE -> {
                outputHandler.invoke(a)
            }
            Operation.CALL -> {
                val currentProgramCounter = state.register(architecture.registers.programCounter)
                stackPush(currentProgramCounter + 1UL)
                result = a
            }
            Operation.CALL_2 -> {
                val currentProgramCounter = state.register(architecture.registers.programCounter)
                val combinedValue = (a and architecture.dataWordType.mask) or ((b and architecture.dataWordType.mask) shl architecture.dataWordType.bits)
                stackPush(currentProgramCounter + 1UL)
                result = combinedValue
            }
            Operation.RETURN -> {
                val poppedValue = stackPop()
                result = poppedValue and outputRegister.type.mask
            }
            Operation.STACK_PUSH -> {
                if (stackPointer() == state.stackState.size - 1) {
                    warningHandler("Stack overflow")
                }
                stackPush(b)
            }
            Operation.STACK_POP -> {
                if (stackPointer() == 0) {
                    warningHandler("Stack underflow")
                }
                val poppedValue = stackPop()
                result = poppedValue and outputRegister.type.mask
            }
            Operation.STACK_PEEK -> {
                result = state.stackPeek() and outputRegister.type.mask
            }
            Operation.BREAK -> {
                hitBreak = true
            }
        }

        return Pair(result, hitBreak)
    }

    private fun applyPatch(patch: ComputerStatePatch<*>) {
        // Remove any existing following history.
        eraseFuture()

        // Add the patch to the history.
        patches += patch
        iStep += 1
    }

    /**
     * Redoes the next step in the history.
     */
    public fun stepForwards(): ComputerStatePatch<*>? {
        if (iStep >= patches.size) {
            return null
        }
        val patch = patches[iStep]
        ++iStep

        state.redoPatch(patch)

        return patch
    }

    /**
     * Reverts the last step in the history.
     */
    public fun stepBackwards(): ComputerStatePatch<*>? {
        if (iStep <= 0) {
            return null
        }
        --iStep
        val patch = patches[iStep]

        state.undoChanges(patch)

        return patch
    }

    public fun eraseFuture() {
        while (iStep <= patches.size - 1) {
            patches.removeAt(iStep)
        }
    }

    public fun numberOfHistorySteps(): Int {
        return patches.size
    }

    public fun currentHistoryStep(): Int {
        return iStep
    }
}
