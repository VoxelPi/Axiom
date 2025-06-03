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
            flag("trace", aliases = arrayOf("t"))
            flag("silent", aliases = arrayOf("s"))

            handler { context ->
                val trace = context.flags().isPresent("trace")
                val silent = context.flags().isPresent("silent")

                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is already running.")
                    return@handler
                }
                computer.run(trace = trace, silent = silent) { nInstructions ->
                    context.sender().lineReader.printAbove("$PREFIX_EMULATOR Computer ${TextColors.brightGreen("finished")} executing ${TextColors.brightYellow(nInstructions.toString())} instructions")
                }.getOrElse {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Failed to run: ${it.message}")
                }
            }
        }

        commandManager.buildAndRegister("run") {
            literal("next")
            optional("n_instructions", integerParser(0))
            flag("trace", aliases = arrayOf("t"))
            flag("silent", aliases = arrayOf("s"))

            handler { context ->
                val nScheduledInstructions: Int = context.getOrDefault("n_instructions", Integer.MAX_VALUE)
                val trace = context.flags().isPresent("trace")
                val silent = context.flags().isPresent("silent")

                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is already running.")
                    return@handler
                }
                computer.run(nScheduledInstructions, trace = trace, silent = silent) { nInstructions ->
                    context.sender().lineReader.printAbove("$PREFIX_EMULATOR Computer ${TextColors.brightGreen("finished")} executing ${TextColors.brightYellow(nInstructions.toString())} instructions")
                }.getOrElse {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Failed to run: ${it.message}")
                }
            }
        }
    }
}
