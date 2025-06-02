package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.cli.util.ValueFormat
import net.voxelpi.axiom.cli.util.formattedValue
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.getOrNull
import org.incendo.cloud.parser.standard.EnumParser.enumParser
import org.incendo.cloud.parser.standard.IntegerParser.integerParser

class EmulatorMemoryCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("memory") {
            required("address", integerParser(0, computer.architecture.memorySize - 1))
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val address: Int = context["address"]
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val value = formattedValue(computerState.memoryCell(address), computer.architecture.memoryWordType, format)
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Memory cell ${TextColors.brightYellow("#$address")} is set to $value")
            }
        }

        commandManager.buildAndRegister("memory") {
            literal("get")
            required("address", integerParser(0, computer.architecture.memorySize - 1))
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val address: Int = context["address"]
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val value = formattedValue(computerState.memoryCell(address), computer.architecture.memoryWordType, format)
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Memory cell ${TextColors.brightYellow("#$address")} is set to $value")
            }
        }

        commandManager.buildAndRegister("memory") {
            literal("dump")
            required("from", integerParser(0, computer.architecture.memorySize - 1))
            required("to", integerParser(0, computer.architecture.memorySize - 1))
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val from: Int = context["from"]
                val to: Int = context["to"]
                val format: ValueFormat? = context.getOrNull("format")

                val computerState = runBlocking { computer.state() }

                val terminal = Terminal()
                terminal.println("${Emulator.PREFIX_EMULATOR} Register dump:")
                if (format == null) {
                    terminal.println(
                        table {
                            header {
                                row("ADDRESS", "DECIMAL", "DECIMAL (signed)", "HEXADECIMAL", "BINARY", "CHARACTER")
                            }
                            body {
                                for (address in from..to) {
                                    val state = computerState.memoryCell(address)
                                    row {
                                        cell(address)
                                        cell(formattedValue(state, computer.architecture.memoryWordType, ValueFormat.DECIMAL))
                                        cell(formattedValue(state, computer.architecture.memoryWordType, ValueFormat.DECIMAL_SIGNED))
                                        cell(formattedValue(state, computer.architecture.memoryWordType, ValueFormat.HEXADECIMAL))
                                        cell(formattedValue(state, computer.architecture.memoryWordType, ValueFormat.BINARY))
                                        cell(formattedValue(state, computer.architecture.memoryWordType, ValueFormat.CHARACTER))
                                    }
                                }
                            }
                        }
                    )
                } else {
                    terminal.println(
                        table {
                            header {
                                row("ADDRESS", "VALUE")
                            }
                            body {
                                for (address in from..to) {
                                    row {
                                        val state = computerState.memoryCell(address)
                                        cell(address)
                                        cell(formattedValue(state, computer.architecture.memoryWordType, format))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
