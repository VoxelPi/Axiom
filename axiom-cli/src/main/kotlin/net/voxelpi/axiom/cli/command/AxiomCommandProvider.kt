package net.voxelpi.axiom.cli.command

interface AxiomCommandProvider {

    fun registerCommands(commandManager: AxiomCommandManager)
}
