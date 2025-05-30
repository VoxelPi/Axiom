package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
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
            literal("back")
            optional("steps", integerParser(1))
            handler { context ->
                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is currently running.")
                }

                val nSteps = context.getOrDefault("steps", 1).coerceAtMost(computer.computer.numberOfHistorySteps())
                repeat(nSteps) {
                    computer.computer.stepBackwards()
                }
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Stepped back ${TextColors.brightYellow(nSteps.toString())} steps.")
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Currently at step ${TextColors.brightYellow(computer.computer.currentHistoryStep().toString())} of ${TextColors.brightYellow(computer.computer.numberOfHistorySteps().toString())}")
            }
        }

        commandManager.buildAndRegister("step") {
            literal("next")
            optional("steps", integerParser(1))
            handler { context ->
                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is currently running.")
                }

                val nSteps = context.getOrDefault("steps", 1).coerceAtMost(computer.computer.numberOfHistorySteps() - computer.computer.currentHistoryStep())
                repeat(nSteps) {
                    computer.computer.stepForwards()
                }
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Stepped forward ${TextColors.brightYellow(nSteps.toString())} steps.")
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Currently at step ${TextColors.brightYellow(computer.computer.currentHistoryStep().toString())} of ${TextColors.brightYellow(computer.computer.numberOfHistorySteps().toString())}")
            }
        }
    }
}
