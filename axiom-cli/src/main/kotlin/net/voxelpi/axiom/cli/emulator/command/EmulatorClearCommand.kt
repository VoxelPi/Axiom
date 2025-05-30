package net.voxelpi.axiom.cli.emulator.command

import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.jline.utils.InfoCmp

object EmulatorClearCommand : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("clear") {
            handler { context ->
                context.sender().terminal.puts(InfoCmp.Capability.clear_screen)
                context.sender().terminal.flush()
                context.sender().terminal.writer().println(Emulator.HEADER_MESSAGE)
            }
        }
    }
}
