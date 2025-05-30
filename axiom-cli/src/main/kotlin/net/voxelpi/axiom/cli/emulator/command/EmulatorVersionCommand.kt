package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.AxiomBuildParameters
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import org.incendo.cloud.kotlin.extension.buildAndRegister

class EmulatorVersionCommand : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("version") {
            handler { context ->
                context.sender().terminal.writer().println("$PREFIX_EMULATOR Running ${TextColors.brightGreen("AXIOM")} v${AxiomBuildParameters.VERSION} ${TextColors.brightBlue("[${AxiomBuildParameters.GIT_BRANCH}]")} ${TextColors.gray("#${AxiomBuildParameters.GIT_COMMIT.substring(0..6)}")}")
            }
        }
    }
}
