package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.command.AxiomCommandSender
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorBreakCommand(val computer: EmulatedComputer) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("break") {
            handler(::haltComputer)
        }
        commandManager.buildAndRegister("halt") {
            handler(::haltComputer)
        }
    }

    private fun haltComputer(context: CommandContext<AxiomCommandSender>) {
        if (!computer.isExecuting()) {
            context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is currently ${TextColors.brightRed("not running")}")
            return
        }
        computer.halt()
        context.sender().terminal.writer().println("$PREFIX_EMULATOR Stopped the computer")
    }
}
