package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.runBlocking
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
                val instructionIndex = state.registerState(computer.architecture.registers.programCounter)
                if (instructionIndex.toInt() in computer.computer.program.instructions.indices) {
                    val instruction = computer.computer.program.instructions[instructionIndex.toInt()]
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR The computer is currently at instruction ${TextColors.yellow(instructionIndex.toString())}, ${TextColors.brightGreen(instruction.toString())}")
                } else {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR The computer is currently at instruction ${TextColors.yellow(instructionIndex.toString())} which is ${TextColors.brightGreen("outside of the program")}")
                }
            }
        }
    }
}
