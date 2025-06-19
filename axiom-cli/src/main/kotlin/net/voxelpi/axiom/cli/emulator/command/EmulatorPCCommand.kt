package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.asm.Assembler
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorPCCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("pc") {
            handler { context ->
                val state = runBlocking { computer.state() }
                val instructionIndex = state.register(computer.architecture.registers.programCounter)
                if (instructionIndex.toInt() in computer.computer.program.data.indices) {
                    val instruction = computer.computer.program.data[instructionIndex.toInt()]
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR The computer is currently at instruction ${TextColors.yellow(instructionIndex.toString())}, ${TextColors.brightGreen(instruction.toString())}")

                    val source = instruction.meta[Assembler.SOURCE_INSTRUCTION_META_KEY] as? SourceLink
                    if (source != null && source is SourceLink.CompilationUnitSlice) {
                        context.sender().terminal.writer().println("$PREFIX_EMULATOR This corresponds to the assembly statement ${TextColors.brightCyan("\"${source.text.trim()}\"")} at ${TextColors.brightYellow("${source.line + 1}")}:${TextColors.brightYellow("${source.column + 1}")} of unit ${TextColors.brightGreen("\"${source.unit.id}\"")}")
                    }
                } else {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR The computer is currently at instruction ${TextColors.yellow(instructionIndex.toString())} which is ${TextColors.brightGreen("outside of the program")}")
                }
            }
        }
    }
}
