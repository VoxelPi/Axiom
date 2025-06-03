package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.cli.util.formattedBooleanValue
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.BooleanParser.booleanParser

class EmulatorCarryCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("carry") {
            literal("get")

            handler { context ->
                val computerState = runBlocking { computer.state() }
                val value = formattedBooleanValue(computerState.carry())
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} ${TextColors.brightYellow("Carry")} is currently set to $value")
            }
        }

        commandManager.buildAndRegister("carry") {
            literal("set")
            required("value", booleanParser())

            handler { context ->
                val value: Boolean = context.get("value")

                val computerState = runBlocking {
                    computer.modifyState {
                        writeCarry(value)
                    }
                }
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} ${TextColors.brightYellow("Carry")} has been set to ${formattedBooleanValue(computerState.carry())}")
            }
        }
    }
}
