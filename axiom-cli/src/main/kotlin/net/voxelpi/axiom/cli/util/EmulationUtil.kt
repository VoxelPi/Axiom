package net.voxelpi.axiom.cli.util

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.state.ComputerStateChange
import net.voxelpi.axiom.computer.state.ComputerStatePatch
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.InstructionValue

private const val INSTRUCTION_NUMBER_LENGTH = 7
private const val SOURCE_LENGTH = 28

private val OPERATION_PART_LENGTH = mapOf(
    WordType.INT8 to 28,
    WordType.INT16 to 32,
    WordType.INT32 to 42,
    WordType.INT64 to 62,
)

private val CONDITION_PART_LENGTH = mapOf(
    WordType.INT8 to 25,
    WordType.INT16 to 27,
    WordType.INT32 to 32,
    WordType.INT64 to 42,
)

private val CHANGE_PART_LENGTH = mapOf(
    WordType.INT8 to 25,
    WordType.INT16 to 27,
    WordType.INT32 to 32,
    WordType.INT64 to 42,
)

fun generateFormattedDescription(patch: ComputerStatePatch<*>, architecture: Architecture): String {
    var description = ""
    val programCounter = architecture.registers.programCounter

    val lengthWithOperation = INSTRUCTION_NUMBER_LENGTH + SOURCE_LENGTH + OPERATION_PART_LENGTH[architecture.dataWordType]!!
    val lengthWithCondition = lengthWithOperation + CONDITION_PART_LENGTH[architecture.dataWordType]!!
    val length = lengthWithCondition + CHANGE_PART_LENGTH[architecture.dataWordType]!!

    val reason = patch.reason
    when (reason) {
        is ComputerStatePatch.Reason.InstructionExecution -> {
            val instructionIndexPart = if (reason is ComputerStatePatch.Reason.InstructionExecution.Program) {
                reason.iInstruction.toString().padStart(5)
            } else {
                "     "
            }
            description += "${TextColors.brightBlue(instructionIndexPart)}${TextColors.gray("  ")}"

            // TODO: Assembler v2
            // val sourceLinkPart = if (reason is ComputerStatePatch.Reason.InstructionExecution.Program) {
            //     val source = reason.instruction.meta[Assembler.SOURCE_INSTRUCTION_META_KEY] as? SourceLink
            //     if (source != null && source is SourceLink.CompilationUnitSlice) {
            //         "${source.unit.id.padStart(16)}  ${"${source.line + 1}".padStart(5)}:${"${source.column + 1}".padStart(2)}"
            //     } else {
            //         " ".repeat(16 + 2 + 5 + 1 + 2)
            //     }
            // } else {
            " ".repeat(16 + 2 + 5 + 1 + 2)
            // }
            // description += "${TextColors.brightYellow(sourceLinkPart)}${TextColors.gray("  ")}"

            val instruction = reason.instruction
            val condition = instruction.condition
            if (condition == Condition.NEVER) {
                description += TextColors.brightMagenta("nop")
                description = description + (TextColors.gray(" ").repeat((length - visibleLength(description)).coerceAtLeast(0)))
                description = TextStyles.underline(description)

                return description
            }

            val inputA = instruction.inputA
            val inputB = instruction.inputB
            val a = "${TextColors.brightGreen("$inputA")}${if (inputA !is InstructionValue.ImmediateValue) TextColors.yellow(":${reason.valueA}") else ""}"
            val b = "${TextColors.brightGreen("$inputB")}${if (inputB !is InstructionValue.ImmediateValue) TextColors.yellow(":${reason.valueB}") else ""}"
            val outputRegister = TextColors.brightGreen(instruction.outputRegister.id)
            description += instruction.operation.asString(outputRegister, a, b)
            description = description + (TextColors.gray(" ").repeat((lengthWithOperation - visibleLength(description)).coerceAtLeast(0)))

            if (instruction.condition != Condition.ALWAYS) {
                val c = "${TextColors.brightGreen("${instruction.conditionRegister}")}${TextColors.yellow(":${reason.valueConditionRegister}")}"
                val conditionResult = if (reason.conditionMet) TextColors.brightGreen("(true)") else TextColors.brightRed("(false)")
                description += "${TextColors.brightMagenta("if")} $c ${instruction.condition.symbol} 0 $conditionResult"
            }
            description = description + (TextColors.gray(" ").repeat((lengthWithCondition - visibleLength(description)).coerceAtLeast(0)))
        }
        ComputerStatePatch.Reason.External -> {
            description += TextColors.gray(" ".repeat(lengthWithCondition))
        }
    }

    description += patch.changes
        .filter {
            if (it is ComputerStateChange.RegisterChange && reason is ComputerStatePatch.Reason.InstructionExecution) {
                if (it.register == programCounter && reason.instruction.outputRegister.register != programCounter) {
                    return@filter false
                }
            }
            return@filter true
        }
        .joinToString(TextColors.gray("  ")) { change ->
            when (change) {
                is ComputerStateChange.CarryChange -> {
                    if (change.newValue == change.previousValue) {
                        TextColors.gray("carry = ${change.newValue}")
                    } else {
                        TextColors.brightBlue("carry = ${change.newValue}")
                    }
                }
                is ComputerStateChange.MemoryChange -> {
                    if (change.newValue == change.previousValue) {
                        TextColors.gray("[${change.address}] = ${change.newValue}")
                    } else {
                        TextColors.brightBlue("[${change.address}] = ${change.newValue}")
                    }
                }
                is ComputerStateChange.RegisterChange -> {
                    if (change.newValue == change.previousValue) {
                        TextColors.gray("${change.register} = ${change.newValue}")
                    } else {
                        TextColors.brightBlue("${change.register} = ${change.newValue}")
                    }
                }
                is ComputerStateChange.Stack.Pop -> TextColors.brightBlue("pop ${change.value}")
                is ComputerStateChange.Stack.Push -> TextColors.brightBlue("push ${change.value}")
                is ComputerStateChange.Stack.Change -> TextColors.brightBlue("stack[${change.address}] = ${change.newValue}")
                is ComputerStateChange.Stack.PointerChange -> TextColors.brightBlue("stack_pointer = ${change.newAddress}")
            }
        }
    description = description + (TextColors.gray(" ").repeat((length - visibleLength(description)).coerceAtLeast(0)))
    description = TextStyles.underline(description)

    return description
}
