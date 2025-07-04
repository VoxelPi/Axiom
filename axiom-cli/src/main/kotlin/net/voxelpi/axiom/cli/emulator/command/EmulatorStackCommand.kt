package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.WordType
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
import org.incendo.cloud.parser.standard.LongParser.longParser

class EmulatorStackCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("stack") {
            literal("peek")
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val depth = formattedValue(computerState.stackPointer().toULong(), WordType.INT64, ValueFormat.DECIMAL)
                if (computerState.stackPointer() == 0) {
                    context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Stack is empty")
                } else {
                    val value = formattedValue(computerState.stackPeek(), computer.architecture.stackWordType, format)
                    context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Stack has a depth of $depth and the top element is set to $value")
                }
            }
        }

        commandManager.buildAndRegister("stack") {
            literal("get")
            required("address", integerParser(0, computer.architecture.stackSize - 1))
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val address: Int = context["address"]
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                if (address >= computerState.stackPointer()) {
                    val depth = formattedValue(computerState.stackPointer().toULong(), WordType.INT64, ValueFormat.DECIMAL)
                    context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Stack is only $depth elements deep")
                } else {
                    val value = formattedValue(computerState.stackCell(address), computer.architecture.stackWordType, format)
                    context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Stack element ${TextColors.brightYellow("#$address")} is set to $value")
                }
            }
        }

        commandManager.buildAndRegister("stack") {
            literal("set")
            required("address", integerParser(0, computer.architecture.stackSize - 1))
            required("value", longParser())

            handler { context ->
                val address: Int = context["address"]
                val value: ULong = context.get<Long>("value").toULong() and computer.architecture.stackWordType.mask

                val computerState = runBlocking {
                    computer.modifyState {
                        writeStackCell(address, value)
                    }
                }
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Stack cell ${TextColors.brightYellow("#$address")} has been set to ${computerState.stackCell(address)}")
            }
        }

        commandManager.buildAndRegister("stack") {
            literal("set")
            literal("pointer")
            required("value", integerParser(0, computer.architecture.stackSize - 1))

            handler { context ->
                val value: Int = context.get("value")

                val computerState = runBlocking {
                    computer.modifyState {
                        writeStackPointer(value)
                    }
                }
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} The ${TextColors.brightYellow("stack pointer")} has been set to ${computerState.stackPointer()}")
            }
        }

        commandManager.buildAndRegister("stack") {
            literal("push")
            required("value", longParser())

            handler { context ->
                val value: ULong = context.get<Long>("value").toULong() and computer.architecture.stackWordType.mask

                val computerState = runBlocking {
                    computer.modifyState {
                        stackPush(value)
                    }
                }
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} The value ${computerState.stackPeek()} has been pushed to the ${TextColors.brightYellow("stack")}")
            }
        }

        commandManager.buildAndRegister("stack") {
            literal("pop")

            handler { context ->
                var poppedValue: ULong? = null
                runBlocking {
                    computer.modifyState {
                        poppedValue = stackPop()
                    }
                }
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} The value $poppedValue has been popped from the ${TextColors.brightYellow("stack")}")
            }
        }

        commandManager.buildAndRegister("stack") {
            literal("dump")
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val format: ValueFormat? = context.getOrNull("format")

                val computerState = runBlocking { computer.state() }
                val depth = computerState.stackPointer()

                val terminal = Terminal()
                terminal.println("${Emulator.PREFIX_EMULATOR} Register dump:")
                if (format == null) {
                    terminal.println(
                        table {
                            header {
                                row("INDEX", "DECIMAL", "DECIMAL (signed)", "HEXADECIMAL", "BINARY", "CHARACTER")
                            }
                            body {
                                for (address in 0 until depth) {
                                    val state = computerState.stackCell(address)!!
                                    row {
                                        cell(address)
                                        cell(formattedValue(state, computer.architecture.stackWordType, ValueFormat.DECIMAL))
                                        cell(formattedValue(state, computer.architecture.stackWordType, ValueFormat.DECIMAL_SIGNED))
                                        cell(formattedValue(state, computer.architecture.stackWordType, ValueFormat.HEXADECIMAL))
                                        cell(formattedValue(state, computer.architecture.stackWordType, ValueFormat.BINARY))
                                        cell(formattedValue(state, computer.architecture.stackWordType, ValueFormat.CHARACTER))
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
                                for (address in 0 until depth) {
                                    row {
                                        val state = computerState.stackCell(address)!!
                                        cell(address)
                                        cell(formattedValue(state, computer.architecture.stackWordType, format))
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
