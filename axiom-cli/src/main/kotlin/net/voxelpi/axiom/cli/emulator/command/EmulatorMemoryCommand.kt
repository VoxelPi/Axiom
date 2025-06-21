package net.voxelpi.axiom.cli.emulator.command

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.arch.MemoryMap
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.command.parser.computerULongParser
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.cli.util.ValueFormat
import net.voxelpi.axiom.cli.util.formattedValue
import net.voxelpi.axiom.computer.state.ComputerState
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.ProgramConstant
import net.voxelpi.axiom.instruction.ProgramElement
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.getOrNull
import org.incendo.cloud.parser.standard.EnumParser.enumParser
import org.incendo.cloud.parser.standard.LongParser.longParser

class EmulatorMemoryCommand(
    val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("memory") {
            required("address", computerULongParser())
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val address = context.get<ULong>("address").toInt()
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val (value, source) = queryMemoryAddress(computerState, address)
                val valueText = value?.let { formattedValue(value, computer.architecture.memoryMap.wordType, format) } ?: EMPTY_STRING
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Memory cell ${TextColors.brightYellow("0x${address.toString(16)}")} is set to $valueText ($source)")
            }
        }

        commandManager.buildAndRegister("memory") {
            literal("get")
            required("address", computerULongParser())
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val address = context.get<ULong>("address").toInt()
                val format: ValueFormat = context.getOrDefault("format", ValueFormat.DECIMAL)

                val computerState = runBlocking { computer.state() }
                val (value, source) = queryMemoryAddress(computerState, address)
                val valueText = value?.let { formattedValue(it, computer.architecture.memoryMap.wordType, format) } ?: EMPTY_STRING
                context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Memory cell ${TextColors.brightYellow("0x${address.toString(16)}")} is set to $valueText ($source)")
            }
        }

        commandManager.buildAndRegister("memory") {
            literal("set")
            required("address", computerULongParser())
            required("value", longParser())

            handler { context ->
                val address = context.get<ULong>("address").toInt()
                val value: ULong = computer.architecture.memoryMap.wordType.unsignedValueOf(context.get<Long>("value").toULong())

                when (val mapping = computer.architecture.memoryMap.selectMapping(address)) {
                    is MemoryMap.MemoryMapping.Memory -> {
                        val memoryCellAddress = mapping.map(address)

                        val computerState = runBlocking {
                            computer.modifyState {
                                writeMemoryCell(memoryCellAddress, value)
                            }
                        }
                        context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Memory cell ${TextColors.brightYellow("0x${address.toString(16)}")} has been set to ${computerState.memoryCell(memoryCellAddress)}")
                    }
                    is MemoryMap.MemoryMapping.Program -> {
                        context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} ${Emulator.PREFIX_ERROR} Unable to modify mapped program memory ${TextColors.brightYellow("0x${address.toString(16)}")}")
                    }
                    null -> {
                        context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} ${Emulator.PREFIX_ERROR} Unable to modify unmapped memory ${TextColors.brightYellow("0x${address.toString(16)}")}")
                    }
                }
            }
        }

        commandManager.buildAndRegister("memory") {
            literal("dump")
            required("from", computerULongParser())
            required("to", computerULongParser())
            optional("format", enumParser(ValueFormat::class.java))

            handler { context ->
                val from: Int = context.get<ULong>("from").toInt()
                val to: Int = context.get<ULong>("to").toInt()
                val format: ValueFormat? = context.getOrNull("format")

                val computerState = runBlocking { computer.state() }

                val terminal = Terminal()
                terminal.println("${Emulator.PREFIX_EMULATOR} Register dump:")
                if (format == null) {
                    terminal.println(
                        table {
                            header {
                                row("ADDRESS", "SOURCE", "DECIMAL", "DECIMAL (signed)", "HEXADECIMAL", "BINARY", "CHARACTER")
                            }
                            body {
                                for (address in from..to) {
                                    val (value, source) = queryMemoryAddress(computerState, address)
                                    row {
                                        cell(TextColors.brightYellow("0x${address.toString(16)}"))
                                        cell(source)
                                        cell(if (value != null) formattedValue(value, computer.architecture.memoryMap.wordType, ValueFormat.DECIMAL) else EMPTY_STRING)
                                        cell(if (value != null) formattedValue(value, computer.architecture.memoryMap.wordType, ValueFormat.DECIMAL_SIGNED) else EMPTY_STRING)
                                        cell(if (value != null) formattedValue(value, computer.architecture.memoryMap.wordType, ValueFormat.HEXADECIMAL) else EMPTY_STRING)
                                        cell(if (value != null) formattedValue(value, computer.architecture.memoryMap.wordType, ValueFormat.BINARY) else EMPTY_STRING)
                                        cell(if (value != null) formattedValue(value, computer.architecture.memoryMap.wordType, ValueFormat.CHARACTER) else EMPTY_STRING)
                                    }
                                }
                            }
                        }
                    )
                } else {
                    terminal.println(
                        table {
                            header {
                                row("ADDRESS", "SOURCE", "VALUE")
                            }
                            body {
                                for (address in from..to) {
                                    row {
                                        val (value, source) = queryMemoryAddress(computerState, address)
                                        cell(TextColors.brightYellow("0x${address.toString(16)}"))
                                        cell(source)
                                        cell(if (value != null) formattedValue(value, computer.architecture.memoryMap.wordType, format) else EMPTY_STRING)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun queryMemoryAddress(computerState: ComputerState, address: Int): Pair<ULong?, String> {
        return when (val mapping = computer.architecture.memoryMap.selectMapping(address)) {
            is MemoryMap.MemoryMapping.Memory -> {
                val mappedAddress = mapping.map(address)
                Pair(computerState.memoryCell(mappedAddress), "memory ${TextColors.brightYellow("0x${mappedAddress.toString(16)}")}")
            }
            is MemoryMap.MemoryMapping.Program -> {
                val mappedAddress = mapping.map(address)
                when (val programElement = computer.computer.program.data[mappedAddress]) {
                    is Instruction -> {
                        val value = computer.architecture.memoryMap.wordType.unpackFirst(computer.architecture.encodeInstruction(programElement).getOrThrow())
                        Pair(value, "program ${TextColors.brightYellow("0x${mappedAddress.toString(16)}")}")
                    }
                    is ProgramConstant -> {
                        val value = computer.architecture.memoryMap.wordType.unsignedValueOf(programElement.value)
                        Pair(value, "program ${TextColors.brightYellow("0x${mappedAddress.toString(16)}")}")
                    }
                    ProgramElement.None -> {
                        Pair(null, "program ${TextColors.brightYellow("0x${mappedAddress.toString(16)}")}")
                    }
                }
            }
            null -> {
                Pair(null, "unmapped")
            }
        }
    }

    companion object {
        private val EMPTY_STRING = TextColors.brightRed("empty")
    }
}
