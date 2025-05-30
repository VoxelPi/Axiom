package net.voxelpi.axiom.cli.emulator.command

import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorBreakCommand(val computer: EmulatedComputer) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("break") {
            handler { context ->
                if (!computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is currently not running.")
                }
                computer.halt()
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Stopped the computer.")
            }
        }
    }
}
