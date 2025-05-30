package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.LongParser.longParser

class EmulatorInputCommand(val inputQueue: ArrayDeque<ULong>) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("input") {
            literal("stage")
            required("value", longParser())

            handler { context ->
                val value: ULong = context.get<Long>("value").toULong()
                inputQueue.addLast(value)
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Added ${TextColors.brightGreen(value.toString())} to the input queue.")
            }
        }

        commandManager.buildAndRegister("input") {
            literal("clear")

            handler { context ->
                inputQueue.clear()
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Cleared the input queue.")
            }
        }
    }
}
