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
import net.voxelpi.axiom.cli.util.stringFromCodePoint
import net.voxelpi.axiom.computer.state.ComputerState
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
                val register: Register<*> = context["register"]

                val computerState = runBlocking { computer.state() }
                val value = formattedRegisterState(register, computerState, RegisterPrintFormat.DECIMAL)
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Register ${TextColors.brightYellow(register.id)} is set to $value")
            }
        }

        commandManager.buildAndRegister("register") {
            required("register", registerParser(computer.architecture.registers))
            literal("get")
            optional("format", enumParser(RegisterPrintFormat::class.java))

            handler { context ->
                val register: Register<*> = context["register"]
                val format: RegisterPrintFormat = context.getOrDefault("format", RegisterPrintFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val value = formattedRegisterState(register, computerState, format)
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Register ${TextColors.brightYellow(register.id)} is set to $value")
            }
        }

        commandManager.buildAndRegister("registers") {
            literal("dump")
            optional("format", enumParser(RegisterPrintFormat::class.java))

            handler { context ->
                val format: RegisterPrintFormat? = context.getOrNull("format")

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
                                    row {
                                        cell(register.id)
                                        cell(formattedRegisterState(register, computerState, RegisterPrintFormat.DECIMAL))
                                        cell(formattedRegisterState(register, computerState, RegisterPrintFormat.DECIMAL_SIGNED))
                                        cell(formattedRegisterState(register, computerState, RegisterPrintFormat.HEXADECIMAL))
                                        cell(formattedRegisterState(register, computerState, RegisterPrintFormat.BINARY))
                                        cell(formattedRegisterState(register, computerState, RegisterPrintFormat.CHARACTER))
                                    }
                                }
                            }
                        }
                    )
                } else {
                    terminal.println(
                        table {
                            header {
                                row("Register", "Value")
                            }
                            body {
                                for (register in computer.architecture.registers.registers.values) {
                                    row {
                                        cell(register.id)
                                        cell(formattedRegisterState(register, computerState, format))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun formattedRegisterState(register: Register<*>, computerState: ComputerState<*>, format: RegisterPrintFormat): String {
        return when (format) {
            RegisterPrintFormat.BINARY -> {
                val state = computerState.registerStateUInt64(register)
                "${TextColors.brightCyan("0b")}${TextColors.brightGreen(state.toString(2).padStart(register.type.bits, '0').chunked(4).joinToString("_"))}"
            }
            RegisterPrintFormat.DECIMAL -> {
                val state = computerState.registerStateUInt64(register)
                TextColors.brightGreen(state.toString())
            }
            RegisterPrintFormat.HEXADECIMAL -> {
                val state = computerState.registerStateUInt64(register)
                "${TextColors.brightCyan("0x")}${TextColors.brightGreen(state.toString(16).padStart(register.type.bytes * 2, '0').chunked(2).joinToString("_").uppercase())}"
            }
            RegisterPrintFormat.DECIMAL_SIGNED -> {
                val state = computerState.registerStateInt64(register)
                TextColors.brightGreen(state.toString())
            }
            RegisterPrintFormat.CHARACTER -> {
                val state = computerState.registerStateUInt64(register)
                val symbol = stringFromCodePoint(state)
                "${TextColors.brightCyan("'")}${TextColors.brightGreen(symbol)}${TextColors.brightCyan("'")}"
            }
        }
    }

    enum class RegisterPrintFormat {
        BINARY,
        DECIMAL,
        HEXADECIMAL,
        DECIMAL_SIGNED,
        CHARACTER,
    }
}
