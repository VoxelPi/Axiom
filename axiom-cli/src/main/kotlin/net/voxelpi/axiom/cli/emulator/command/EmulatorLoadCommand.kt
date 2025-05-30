package net.voxelpi.axiom.cli.emulator.command

import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.suggestionProvider
import org.incendo.cloud.parser.standard.StringParser.quotedStringParser
import org.incendo.cloud.suggestion.SuggestionProvider
import java.io.File

class EmulatorLoadCommand(val emulator: Emulator) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("load") {
            required("program", quotedStringParser()) {
                suggestionProvider = SuggestionProvider.blockingStrings { _, _ ->
                    File(".").walkTopDown()
                        .filter { it.isFile && it.extension == "axm" }
                        .map { it.toString() }
                        .toList()
                }
            }

            handler { context ->
                val inputFile: String = context.get("program")
                emulator.loadProgram(inputFile)
            }
        }

        commandManager.buildAndRegister("reload") {
            handler {
                emulator.reloadProgram()
            }
        }
    }
}
