package net.voxelpi.axiom.cli.command

import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler

class AxiomCommandManager : CommandManager<AxiomCommandSender>(
    ExecutionCoordinator.simpleCoordinator(),
    CommandRegistrationHandler.nullCommandRegistrationHandler(),
) {

    override fun hasPermission(sender: AxiomCommandSender, permission: String): Boolean {
        return true
    }

    public fun registerCommands(commandProvider: AxiomCommandProvider) {
        commandProvider.registerCommands(this)
    }
}
