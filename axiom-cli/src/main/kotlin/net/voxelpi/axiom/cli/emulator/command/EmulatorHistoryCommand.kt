package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_COMPUTER
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.cli.util.generateFormattedDescription
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser.integerParser

class EmulatorHistoryCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("history") {
            handler { context ->
                val iStep = computer.computer.currentHistoryStep()
                val nSteps = computer.computer.numberOfHistorySteps()
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Currently at step ${TextColors.brightYellow(iStep.toString())} of ${TextColors.brightYellow(nSteps.toString())}")
            }
        }

        commandManager.buildAndRegister("step") {
            optional("steps", integerParser())
            handler { context ->
                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is currently running.")
                    return@handler
                }

                val nStepsRaw = context.getOrDefault("steps", 1)
                if (nStepsRaw < 0) {
                    // Step backwards.
                    val nSteps = (-nStepsRaw).coerceAtMost(computer.computer.numberOfHistorySteps())
                    repeat(nSteps) {
                        computer.computer.stepBackwards()?.let { result ->
                            context.sender().terminal.writer().println("$PREFIX_COMPUTER ${TextColors.brightCyan("[UNDO]")}  ${generateFormattedDescription(result, computer.architecture)}")
                        }
                    }
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Stepped back ${TextColors.brightYellow(nSteps.toString())} steps.")
                } else if (nStepsRaw > 0) {
                    // Step forward.
                    val nSteps = nStepsRaw.coerceAtMost(computer.computer.numberOfHistorySteps() - computer.computer.currentHistoryStep())
                    repeat(nSteps) {
                        computer.computer.stepForwards()?.let { result ->
                            context.sender().terminal.writer().println("$PREFIX_COMPUTER ${TextColors.brightCyan("[REDO]")}  ${generateFormattedDescription(result, computer.architecture)}")
                        }
                    }
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Stepped forward ${TextColors.brightYellow(nSteps.toString())} steps.")
                }
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Now at step ${TextColors.brightYellow(computer.computer.currentHistoryStep().toString())} of ${TextColors.brightYellow(computer.computer.numberOfHistorySteps().toString())}")
            }
        }
    }
}
