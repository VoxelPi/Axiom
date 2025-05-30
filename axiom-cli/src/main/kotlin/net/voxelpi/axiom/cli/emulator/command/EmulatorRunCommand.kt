package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser.integerParser

class EmulatorRunCommand(val computer: EmulatedComputer) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("run") {
            optional("n_instructions", integerParser(0))

            handler { context ->
                val nScheduledInstructions: Int = context.getOrDefault("n_instructions", Integer.MAX_VALUE)

                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is already running.")
                }
                computer.run(nScheduledInstructions) { nInstructions ->
                    context.sender().lineReader.printAbove("$PREFIX_EMULATOR Computer finished executing ${TextColors.brightCyan(nInstructions.toString())} instructions")
                }.getOrElse {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Failed to run: ${it.message}")
                }
            }
        }
    }
}
