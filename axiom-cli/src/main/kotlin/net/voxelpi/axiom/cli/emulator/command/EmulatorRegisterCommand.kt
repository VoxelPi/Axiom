package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.command.parser.registerParser
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.register.Register
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.EnumParser.enumParser

class EmulatorRegisterCommand(
    val emulator: Emulator,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("register") {
            required("register", registerParser(emulator.architecture.registers))

            handler { context ->
                val register: Register<*> = context["register"]
                val state = emulator.computer.currentState().registerStateUInt64(register)
                println("Register ${TextColors.brightYellow(register.id)} is set to ${TextColors.brightGreen(state.toString())}")
            }
        }

        commandManager.buildAndRegister("register") {
            required("register", registerParser(emulator.architecture.registers))
            literal("get")
            optional("format", enumParser(RegisterPrintFormat::class.java))

            handler { context ->
                val register: Register<*> = context["register"]
                val format: RegisterPrintFormat = context.getOrDefault("format", RegisterPrintFormat.DECIMAL)

                val value: String = when (format) {
                    RegisterPrintFormat.BINARY -> {
                        val state = emulator.computer.currentState().registerStateUInt64(register)
                        "${TextColors.brightCyan("0b")}${TextColors.brightGreen(state.toString(2).padStart(register.type.bits, '0').chunked(4).joinToString("_"))}"
                    }
                    RegisterPrintFormat.DECIMAL -> {
                        val state = emulator.computer.currentState().registerStateUInt64(register)
                        TextColors.brightGreen(state.toString())
                    }
                    RegisterPrintFormat.HEXADECIMAL -> {
                        val state = emulator.computer.currentState().registerStateUInt64(register)
                        "${TextColors.brightCyan("0x")}${TextColors.brightGreen(state.toString(16).padStart(register.type.bytes * 2, '0').chunked(2).joinToString("_").uppercase())}"
                    }
                    RegisterPrintFormat.DECIMAL_SIGNED -> {
                        val state = emulator.computer.currentState().registerStateInt64(register)
                        TextColors.brightGreen(state.toString())
                    }
                    RegisterPrintFormat.CHARACTER -> {
                        val state = emulator.computer.currentState().registerStateUInt64(register).toUInt().toInt()
                        "${TextColors.brightCyan("'")}${TextColors.brightGreen(Character.toChars(state).concatToString())}${TextColors.brightCyan("'")}"
                    }
                }

                println("Register ${TextColors.brightYellow(register.id)} is set to $value")
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
