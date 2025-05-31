package net.voxelpi.axiom.computer

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.state.ComputerState
import net.voxelpi.axiom.computer.state.ComputerStateChange
import net.voxelpi.axiom.computer.state.MutableComputerState
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.instruction.Program
import kotlin.math.sqrt

public class Computer<P : Comparable<P>>(
    public val architecture: Architecture<P, *>,
    public val inputAvailableProvider: () -> Boolean,
    public val inputProvider: () -> ULong,
    public val outputHandler: (ULong) -> Unit,
) {

    public var program: Program = Program(emptyList())
        private set

    private val state: MutableComputerState<P> = MutableComputerState(architecture)

    private var iStep = 0
    private val steps: MutableList<InstructionExecutionResult> = mutableListOf()

    public fun loadProgram(program: Program) {
        this.program = program
        reset()
    }

    public fun reset() {
        state.clear()
        iStep = 0
        steps.clear()
    }

    public fun currentState(): ComputerState<P> {
        return state
    }

    public fun runUntilBreak(): Int {
        var nExecutedInstructions = 0
        while (true) {
            val result = runSingleInstruction()
            ++nExecutedInstructions
            if (result.hitBreak) {
                break
            }
        }
        return nExecutedInstructions
    }

    public fun runSingleInstruction(): InstructionExecutionResult {
        val changes: MutableList<ComputerStateChange> = mutableListOf()

        var hitBreak = false

        // FETCH
        val instructionIndex = state.registerStateUInt64(architecture.registers.programCounter)
        val instruction = if (instructionIndex.toInt() in program.instructions.indices) {
            program.instructions[instructionIndex.toInt()]
        } else {
            hitBreak = true
            val operation = if (architecture.dataWordType < architecture.registers.programCounterVariable.type) {
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
            )
        }

        // DECODE
        val operation = instruction.operation
        val condition = instruction.condition
        val a: ULong = when (val value = instruction.inputA) {
            is InstructionValue.ImmediateValue -> value.value.toULong()
            is InstructionValue.RegisterReference -> state.registerVariableStateUInt64(value.register)
        }
        val b: ULong = when (val value = instruction.inputB) {
            is InstructionValue.ImmediateValue -> value.value.toULong()
            is InstructionValue.RegisterReference -> state.registerVariableStateUInt64(value.register)
        }
        val c: ULong = state.registerVariableStateUInt64(instruction.conditionRegister)
        val cSize = instruction.conditionRegister.type

        val outputRegister = instruction.outputRegister

        val carryIn = state.carryState

        // CONDITION
        val conditionValid: Boolean = when (condition) {
            Condition.ALWAYS -> true
            Condition.NEVER -> false
            Condition.EQUAL -> c == 0UL
            Condition.NOT_EQUAL -> c != 0UL
            Condition.LESS -> ((c shl (cSize.bits - 1)) and 1UL) != 0UL
            Condition.LESS_OR_EQUAL -> c == 0UL || ((c shl (cSize.bits - 1)) and 1UL) != 0UL
            Condition.GREATER -> c != 0UL && ((c shl (cSize.bits - 1)) and 1UL) == 0UL
            Condition.GREATER_OR_EQUAL -> ((c shl (cSize.bits - 1)) and 1UL) == 0UL
        }

        // EXECUTE
        var result = 0UL
        var hasResult = true
        var carryOut: Boolean? = null
        if (conditionValid) {
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
                    carryOut = result < a || result < b
                }
                Operation.SUBTRACT -> {
                    val bInput = (b.inv() + 1UL) and architecture.dataWordType.mask
                    result = (a + bInput) and outputRegister.type.mask
                    carryOut = result < a || result < bInput
                }
                Operation.ADD_WITH_CARRY -> {
                    // A + B + Cin
                    val carryState = if (carryIn) 1UL else 0UL
                    val partialSum = (a + b) and outputRegister.type.mask
                    result = (a + b + carryState) and outputRegister.type.mask
                    carryOut = result < a || result < b || result < partialSum
                }
                Operation.SUBTRACT_WITH_CARRY -> {
                    // A - B - (1 - C_in) = A - (B + 1 - C_in) = A + (inv(B) + 1) + inv(C_in))
                    val bInput = (b.inv() + 1UL) and architecture.dataWordType.mask
                    val carryState = if (carryIn) 0UL else 1UL // INVERTED!!!

                    val partialSum = (a + bInput) and outputRegister.type.mask
                    result = (a + bInput + carryState) and outputRegister.type.mask
                    carryOut = result < a || result < bInput || result < partialSum
                }
                Operation.INCREMENT -> {
                    carryOut = (a == outputRegister.type.mask)
                    result = (a + 1UL) and outputRegister.type.mask
                }
                Operation.DECREMENT -> {
                    carryOut = a != 0UL
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
                    carryOut = (a shr (architecture.dataWordType.bits - 1)) and 1UL != 0UL
                }
                Operation.SHIFT_RIGHT -> {
                    result = (a shr 1) and outputRegister.type.mask
                    carryOut = ((a shr 0) and 1UL) != 0UL
                }
                Operation.ROTATE_LEFT -> {
                    val carryState = if (carryIn) 1UL else 0UL
                    result = ((a shl 1) or (carryState shl 0)) and outputRegister.type.mask
                    carryOut = (a shr (architecture.dataWordType.bits - 1)) and 1UL != 0UL
                }
                Operation.ROTATE_RIGHT -> {
                    val carryState = if (carryIn) 1UL else 0UL
                    result = ((a shr 1) or (carryState shl (architecture.dataWordType.bits - 1))) and outputRegister.type.mask
                    carryOut = ((a shr 0) and 1UL) != 0UL
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
                    val address = a.toInt().coerceIn(0, architecture.memorySize - 1)
                    val value = state.memoryState(address)
                    result = value and outputRegister.type.mask
                }
                Operation.MEMORY_STORE -> {
                    val address = a.toInt().coerceIn(0, architecture.memorySize - 1)
                    changes += state.makeMemoryModification(address, b)
                    hasResult = false
                }
                Operation.IO_POLL -> {
                    result = 1UL
                }
                Operation.IO_READ -> {
                    result = inputProvider() and outputRegister.type.mask
                }
                Operation.IO_WRITE -> {
                    outputHandler.invoke(a)
                    hasResult = false
                }
                Operation.CALL -> {
                    val currentProgramCounter = state.registerStateUInt64(architecture.registers.programCounter)
                    changes += state.makeStackPush(currentProgramCounter + 1UL)
                    result = a
                }
                Operation.CALL_2 -> {
                    val currentProgramCounter = state.registerStateUInt64(architecture.registers.programCounter)
                    val combinedValue = (a and architecture.dataWordType.mask) or ((b and architecture.dataWordType.mask) shl architecture.dataWordType.bits)
                    changes += state.makeStackPush(currentProgramCounter + 1UL)
                    result = combinedValue
                }
                Operation.RETURN -> {
                    val (change, poppedValue) = state.makeStackPop()
                    result = poppedValue and outputRegister.type.mask
                    changes += change
                }
                Operation.STACK_PUSH -> {
                    changes += state.makeStackPush(b)
                    hasResult = false
                }
                Operation.STACK_POP -> {
                    val (change, poppedValue) = state.makeStackPop()
                    result = poppedValue and outputRegister.type.mask
                    changes += change
                }
                Operation.STACK_PEEK -> {
                    result = state.stackPeek() and outputRegister.type.mask
                }
                Operation.BREAK -> {
                    hitBreak = true
                    hasResult = false
                }
            }
        }

        // INCREMENT
        if (!conditionValid || !hasResult || outputRegister.register.id != architecture.registers.programCounter.id) {
            val nextProgramCounter = (instructionIndex + 1UL) and architecture.registers.programCounter.type.mask
            val change = state.makeRegisterModification(architecture.registers.programCounter, nextProgramCounter)
            changes += change
        }

        // STORE
        if (conditionValid and hasResult) {
            val change = state.makeRegisterVariableModification(outputRegister, result)
            changes += change

            // Update carry if neccessary.
            if (carryOut != null) {
                changes += state.makeCarryModification(carryOut)
            }
        }

        // Remove any existing following history.
        while (iStep <= steps.size - 1) {
            steps.removeAt(iStep)
        }

        // Create the execution result.
        val executionResult = InstructionExecutionResult(
            instructionIndex.toInt(),
            instruction,
            a,
            b,
            if (conditionValid && hasResult) result else null,
            c,
            conditionMet = conditionValid,
            changes,
            hitBreak = hitBreak,
        )

        // Add the execution result to the history.
        steps += executionResult
        iStep += 1

        // Create and return execution result.
        return executionResult
    }

    /**
     * Redoes the next step in the history.
     */
    public fun stepForwards(): InstructionExecutionResult? {
        if (iStep >= steps.size) {
            return null
        }
        val step = steps[iStep]
        ++iStep

        state.redoChanges(step.changes)

        return step
    }

    /**
     * Reverts the last step in the history.
     */
    public fun stepBackwards(): InstructionExecutionResult? {
        if (iStep <= 0) {
            return null
        }
        --iStep
        val step = steps[iStep]

        state.undoChanges(step.changes)

        return step
    }

    public fun numberOfHistorySteps(): Int {
        return steps.size
    }

    public fun currentHistoryStep(): Int {
        return iStep
    }
}
