package net.voxelpi.axiom.cli.util

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.InstructionExecutionResult
import net.voxelpi.axiom.computer.state.ComputerStateChange
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.InstructionValue

fun generateFormattedDescription(result: InstructionExecutionResult, architecture: Architecture<*, *>): String {
    val programCounter = architecture.registers.programCounter

    var description = "${TextColors.brightBlue(result.iInstruction.toString().padStart(5))}${TextColors.gray("  ")}"

    val instruction = result.instruction
    val condition = instruction.condition
    if (condition == Condition.NEVER) {
        description += TextColors.brightMagenta("nop")
        description = description + (TextColors.gray(" ").repeat((160 - visibleLength(description)).coerceAtLeast(0)))
        description = TextStyles.underline(description)

        return description
    }

    val inputA = instruction.inputA
    val inputB = instruction.inputB
    val a = "${TextColors.brightGreen("$inputA")}${if (inputA !is InstructionValue.ImmediateValue) TextColors.yellow(":${result.valueA}") else ""}"
    val b = "${TextColors.brightGreen("$inputB")}${if (inputB !is InstructionValue.ImmediateValue) TextColors.yellow(":${result.valueB}") else ""}"
    val outputRegister = TextColors.brightGreen(instruction.outputRegister.id)
    description += instruction.operation.asString(outputRegister, a, b)
    description = description + (TextColors.gray(" ").repeat((69 - visibleLength(description)).coerceAtLeast(0)))

    if (instruction.condition != Condition.ALWAYS) {
        val c = "${TextColors.brightGreen("${instruction.conditionRegister}")}${TextColors.yellow(":${result.valueConditionRegister}")}"
        val conditionResult = if (result.conditionMet) TextColors.brightGreen("(true)") else TextColors.brightRed("(false)")
        description += "${TextColors.brightMagenta("if")} $c ${instruction.condition.symbol} 0 $conditionResult"
    }
    description = description + (TextColors.gray(" ").repeat((111 - visibleLength(description)).coerceAtLeast(0)))

    description += result.changes
        .filter { it !is ComputerStateChange.RegisterChange || it.register != programCounter || instruction.outputRegister.register == programCounter }
        .joinToString(TextColors.gray("  ")) { change ->
            val changeDescription = when (change) {
                is ComputerStateChange.CarryChange -> "carry = ${change.newValue}"
                is ComputerStateChange.MemoryChange -> "[${change.address}] = ${change.newValue}"
                is ComputerStateChange.RegisterChange -> "${change.register} = ${change.newValue}"
                is ComputerStateChange.StackPop -> "pop ${change.value}"
                is ComputerStateChange.StackPush -> "push ${change.value}"
            }
            TextColors.brightBlue(changeDescription)
        }
    description = description + (TextColors.gray(" ").repeat((160 - visibleLength(description)).coerceAtLeast(0)))
    description = TextStyles.underline(description)

    return description
}
