package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.AxiomBuildParameters
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorVersionCommand : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("version") {
            handler { context ->
                println("${TextColors.brightGreen("AXIOM")} v${AxiomBuildParameters.VERSION} (${AxiomBuildParameters.GIT_BRANCH}) ${TextColors.gray("#${AxiomBuildParameters.GIT_COMMIT.substring(0..6)}")}")
            }
        }
    }
}
