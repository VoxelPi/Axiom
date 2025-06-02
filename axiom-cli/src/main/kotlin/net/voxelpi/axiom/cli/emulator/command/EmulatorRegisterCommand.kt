package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.command.parser.registerParser
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.cli.util.ValueFormat
import net.voxelpi.axiom.cli.util.formattedValue
import net.voxelpi.axiom.register.Register
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.getOrNull
import org.incendo.cloud.parser.standard.EnumParser.enumParser

class EmulatorRegisterCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("register") {
            required("register", registerParser(computer.architecture.registers))

            handler { context ->
                val register: Register = context["register"]

                val computerState = runBlocking { computer.state() }
                val value = formattedValue(computerState.register(register), register.type, ValueFormat.DECIMAL)
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Register ${TextColors.brightYellow(register.id)} is set to $value")
            }
        }

        commandManager.buildAndRegister("register") {
            required("register", registerParser(computer.architecture.registers))
            literal("get")
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val register: Register = context["register"]
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val value = formattedValue(computerState.register(register), register.type, format)
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Register ${TextColors.brightYellow(register.id)} is set to $value")
            }
        }

        commandManager.buildAndRegister("registers") {
            literal("dump")
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val format: ValueFormat? = context.getOrNull("format")

                val computerState = runBlocking { computer.state() }

                val terminal = Terminal()
                terminal.println("${Emulator.PREFIX_EMULATOR} Register dump:")
                if (format == null) {
                    terminal.println(
                        table {
                            header {
                                row("REGISTER", "DECIMAL", "DECIMAL (signed)", "HEXADECIMAL", "BINARY", "CHARACTER")
                            }
                            body {
                                for (register in computer.architecture.registers.registers.values) {
                                    val state = computerState.register(register)
                                    row {
                                        cell(register.id)
                                        cell(formattedValue(state, register.type, ValueFormat.DECIMAL))
                                        cell(formattedValue(state, register.type, ValueFormat.DECIMAL_SIGNED))
                                        cell(formattedValue(state, register.type, ValueFormat.HEXADECIMAL))
                                        cell(formattedValue(state, register.type, ValueFormat.BINARY))
                                        cell(formattedValue(state, register.type, ValueFormat.CHARACTER))
                                    }
                                }
                            }
                        }
                    )
                } else {
                    terminal.println(
                        table {
                            header {
                                row("REGISTER", "VALUE")
                            }
                            body {
                                for (register in computer.architecture.registers.registers.values) {
                                    row {
                                        cell(register.id)
                                        cell(formattedValue(computerState.register(register), register.type, format))
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
