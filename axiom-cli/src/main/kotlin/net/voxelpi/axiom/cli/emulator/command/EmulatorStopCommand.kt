package net.voxelpi.axiom.cli.emulator.command

import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorStopCommand(
    val emulator: Emulator,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("stop") {
            handler { context ->
                emulator.stop()
            }
        }
    }
}
