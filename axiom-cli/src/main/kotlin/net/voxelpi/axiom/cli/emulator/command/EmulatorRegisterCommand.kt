package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.command.parser.registerParser
import net.voxelpi.axiom.cli.emulator.Emulator.Companion.PREFIX_EMULATOR
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.computer.state.ComputerState
import net.voxelpi.axiom.register.Register
import org.incendo.cloud.kotlin.extension.buildAndRegister
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
                printRegisterState(register, computerState, RegisterPrintFormat.DECIMAL)
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
                printRegisterState(register, computerState, format)
            }
        }
    }

    private fun printRegisterState(register: Register<*>, computerState: ComputerState<*>, format: RegisterPrintFormat) {
        val value: String = when (format) {
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
                val state = computerState.registerStateUInt64(register).toUInt().toInt()
                "${TextColors.brightCyan("'")}${TextColors.brightGreen(Character.toChars(state).concatToString())}${TextColors.brightCyan("'")}"
            }
        }

        println("$PREFIX_EMULATOR Register ${TextColors.brightYellow(register.id)} is set to $value")
    }

    enum class RegisterPrintFormat {
        BINARY,
        DECIMAL,
        HEXADECIMAL,
        DECIMAL_SIGNED,
        CHARACTER,
    }
}
