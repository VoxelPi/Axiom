package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.kotlin.extension.argumentDescription
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import kotlin.jvm.optionals.getOrNull

class EmulatorRunCommand(val computer: EmulatedComputer) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("run") {
            flag("trace", aliases = arrayOf("t"))
            flag("silent", aliases = arrayOf("s"))
            flag("number", aliases = arrayOf("n"), argumentDescription("Number of instructions to execute."), integerParser(0))

            handler { context ->
                val trace = context.flags().isPresent("trace")
                val silent = context.flags().isPresent("silent")
                val nScheduledInstructions = context.flags().getValue<Int>("number").getOrNull()

                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is already running.")
                    return@handler
                }
                computer.run(nScheduledInstructions ?: Int.MAX_VALUE, trace = trace, silent = silent) { nInstructions ->
                    if (nScheduledInstructions != null && nInstructions < nScheduledInstructions) {
                        context.sender().lineReader.printAbove("$PREFIX_EMULATOR Computer entered ${TextColors.brightRed("break")} after executing ${TextColors.brightYellow(nInstructions.toString())} / ${TextColors.brightYellow(nScheduledInstructions.toString())} instructions")
                    } else {
                        context.sender().lineReader.printAbove("$PREFIX_EMULATOR Computer ${TextColors.brightGreen("finished")} executing ${TextColors.brightYellow(nInstructions.toString())} instructions")
                    }
                }.getOrElse {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Failed to run: ${it.message}")
                }
            }
        }
    }
}
