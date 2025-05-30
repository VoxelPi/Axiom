package net.voxelpi.axiom.cli.emulator.command

import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorResetCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("reset") {
            handler { context ->
                if (computer.isExecuting()) {
                    context.sender().terminal.writer().println("$PREFIX_EMULATOR Computer is currently running.")
                    return@handler
                }
                computer.reset()
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Reset the emulator")
            }
        }
    }
}
