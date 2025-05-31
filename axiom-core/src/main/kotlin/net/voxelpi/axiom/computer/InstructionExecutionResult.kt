package net.voxelpi.axiom.computer

import net.voxelpi.axiom.computer.state.ComputerStateChange
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue

public data class InstructionExecutionResult(
    val iInstruction: Int,
    val instruction: Instruction,
    val valueA: ULong,
    val valueB: ULong,
    val output: ULong?,
    val valueConditionRegister: ULong,
    val conditionMet: Boolean,
    val changes: List<ComputerStateChange>,
    val hitBreak: Boolean,
) {

    public fun description(): String {
        val a = if (instruction.inputA is InstructionValue.ImmediateValue) instruction.inputA.value.toString() else "${instruction.inputA}|$valueA"
        val b = if (instruction.inputB is InstructionValue.ImmediateValue) instruction.inputB.value.toString() else "${instruction.inputB}|$valueB"

        return when (instruction.condition) {
            Condition.ALWAYS -> "$iInstruction ${instruction.operation.asString(instruction.outputRegister.id, a, b)}${if (output != null) " (${output})" else ""}"
            Condition.NEVER -> "nop"
            else -> {
                "$iInstruction ${instruction.operation.asString(instruction.outputRegister.id, a, b)}${if (output != null) " (=${output})" else ""} if ${instruction.conditionRegister}|$valueConditionRegister ${instruction.condition.symbol} 0 (=${conditionMet})"
            }
        }
    }
}
