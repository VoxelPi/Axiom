package net.voxelpi.axiom.cli

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Text
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.arch.ax08.AX08Architecture
import net.voxelpi.axiom.arch.dev64.DEV64Architecture
import net.voxelpi.axiom.arch.mcpc08.MCPC08Architecture
import net.voxelpi.axiom.arch.mcpc16.MCPC16Architecture
import net.voxelpi.axiom.asm.Assembler
import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.instruction.Program
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.system.exitProcess

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    val parser = ArgParser("axiom")
    val terminal = Terminal()

    val architectures: Map<String, Architecture<*, *>> = listOf(
        AX08Architecture,
        DEV64Architecture,
        MCPC08Architecture,
        MCPC16Architecture,
    ).associateBy(Architecture<*, *>::id)

    class Assemble : Subcommand("assemble", "Assembles a program.") {
        val input by argument(ArgType.String, description = "The input file.")
        val output by option(ArgType.String, "output", "o", description = "The output file.")

        val architecture by option(
            ArgType.Choice(choices = architectures.values.toList(), toVariant = { architectures[it]!! }),
            fullName = "architecture",
            shortName = "a",
            description = "The architecture to use."
        ).default(AX08Architecture)

        override fun execute() {
            val inputFilePath = Path(input).absolute().normalize()
            if (!inputFilePath.exists() || !inputFilePath.isRegularFile()) {
                println("The input file $inputFilePath does not exist.")
                exitProcess(1)
            }

            val outputFilePath = (output?.let { Path(it) } ?: inputFilePath.parent.resolve(inputFilePath.nameWithoutExtension + ".bin")).normalize()
            println("Assembling \"${inputFilePath.absolutePathString()}\" to \"${outputFilePath.absolutePathString()}\"")

            val rawFile = outputFilePath.parent / "${outputFilePath.nameWithoutExtension}.raw.${Assembler.AXIOM_ASM_EXTENSION}"

            val assembler = Assembler(listOf(inputFilePath.parent.absolute().normalize()))

            val program = assembler.assemble(inputFilePath, architecture).getOrElse { exception ->
                terminal.println(Text(TextColors.brightRed(TextStyles.bold("COMPILATION FAILED"))), true)
                printCompilationStackTrace(exception, terminal)
                exitProcess(1)
            }

            rawFile.writeText(program.toString())

            if (architecture.hasEncodedFormat) {
                val encodedProgram = architecture.encodeProgram(program).getOrThrow()
                outputFilePath.writeBytes(encodedProgram.toByteArray())
                println("Assembled ${program.instructions.size} instructions (${encodedProgram.size} bytes)")
            } else {
                println("Assembled ${program.instructions.size} instructions")
            }
        }
    }

    class Emulate() : Subcommand("emulate", "Emulates a program.") {
        val input by argument(ArgType.String, description = "The input file.")

        val architecture by option(
            ArgType.Choice(choices = architectures.values.toList(), toVariant = { architectures[it]!! }),
            fullName = "architecture",
            shortName = "a",
            description = "The architecture to use."
        ).default(AX08Architecture)

        override fun execute() {
            val inputFilePath = Path(input).absolute().normalize()
            if (!inputFilePath.exists() || !inputFilePath.isRegularFile()) {
                println("The input file $inputFilePath does not exist.")
                exitProcess(1)
            }
            println("Emulating \"${inputFilePath.absolutePathString()}\"")

            val assembler = Assembler(listOf(inputFilePath.parent.absolute().normalize()))

            val program: Program = assembler.assemble(inputFilePath, architecture).getOrElse { exception ->
                terminal.println(Text(TextColors.brightRed(TextStyles.bold("COMPILATION FAILED"))), true)
                printCompilationStackTrace(exception, terminal)
                exitProcess(1)
            }

            val emulator = Emulator(architecture, program)
            emulator.start()

            // val inputProvider: () -> ULong = {
            //     var result: ULong
            //     while (true) {
            //         print("[INPUT] > ")
            //         val input = readln()
            //
            //         if (input.length == 3 && input[0] == '\'' && input[2] == '\'') {
            //             result = input[1].code.toULong()
            //             break
            //         }
            //
            //         val integer = parseInteger(input)
            //         if (integer != null) {
            //             result = integer.toULong()
            //             break
            //         }
            //         println("[ERROR] INVALID INPUT: \"$input\". Please enter a valid integer or character.")
            //     }
            //
            //     result
            // }
            //
            // val outputHandler: (ULong) -> Unit = { value ->
            //     val signedValue = when (architecture.dataWordType) {
            //         WordType.INT8 -> value.toByte()
            //         WordType.INT16 -> value.toShort()
            //         WordType.INT32 -> value.toInt()
            //         WordType.INT64 -> value.toLong()
            //     }
            //     println("[OUTPUT] uint: $value   int: $signedValue   char: '${value.toInt().toChar()}'")
            // }
            //
            // val emulator = EmulatedComputer(architecture, program, { true }, inputProvider, outputHandler)
            // while (true) {
            //     emulator.runUntilBreak()
            //     readlnOrNull() ?: break
            // }
        }
    }

    parser.subcommands(Assemble(), Emulate())
    parser.parse(args)
}

private val sourceTextStyle = TextColors.brightCyan
private val sourceRefStyle = TextColors.brightYellow
private val sourceUnitStyle = TextColors.brightGreen
private val errorStyle = TextColors.brightRed

private fun errorSourceText(source: SourceLink.CompilationUnitSlice): String {
    var iStatementStart = source.index
    while (iStatementStart > 0) {
        --iStatementStart
        if (source.unit.content[iStatementStart] in listOf('\n', '\r', ';')) {
            ++iStatementStart
            break
        }
    }
    while (iStatementStart < source.index + source.length) {
        if (!source.unit.content[iStatementStart].isWhitespace()) {
            break
        }
        ++iStatementStart
    }

    var iStatementEnd = source.index + source.length
    while (iStatementEnd < source.unit.content.length) {
        if (source.unit.content[iStatementEnd] in listOf('\n', '\r', ';')) {
            break
        }
        ++iStatementEnd
    }
    while (iStatementEnd > iStatementStart) {
        if (!source.unit.content[iStatementEnd - 1].isWhitespace()) {
            break
        }
        --iStatementEnd
    }

    val statementText = source.unit.content.substring(iStatementStart, iStatementEnd)
    val highlightStart = (source.index - iStatementStart).coerceAtLeast(0)
    val highlightEnd = highlightStart + (source.length).coerceAtMost(iStatementEnd - iStatementStart)
    return "\"${statementText.substring(0, highlightStart)}${TextStyles.underline(statementText.substring(highlightStart, highlightEnd))}${statementText.substring(highlightEnd)}\""
}

private fun printCompilationStackTrace(exception: Throwable, terminal: Terminal, depth: Int = 0) {
    val prefix = TextColors.gray("${"    ".repeat(depth)}  └ ")
    when (exception) {
        is ParseException -> {
            val source = exception.source
            when (source) {
                is SourceLink.CompilationUnitSlice -> {
                    terminal.println(Text("$prefix Failed to parse ${sourceTextStyle(errorSourceText(source))} at ${sourceRefStyle("${source.line + 1}")}:${sourceRefStyle("${source.column + 1}")} of unit ${sourceUnitStyle("\"${source.unit.id}\"")}: ${errorStyle(exception.message ?: "")}"), true)
                }
                is SourceLink.Generated -> {
                    terminal.println(Text("$prefix Failed to parse ${sourceTextStyle("\"${source.text}\"")} ${sourceRefStyle("generated by \"${source.generator}\"")}: ${errorStyle(exception.message ?: "")}"), true)
                }
            }
        }
        is SourceCompilationException -> {
            val source = exception.source
            when (source) {
                is SourceLink.CompilationUnitSlice -> {
                    terminal.println(Text("$prefix Failed to compile ${sourceTextStyle(errorSourceText(source))} at ${sourceRefStyle("${source.line + 1}")}:${sourceRefStyle("${source.column + 1}")} of unit ${sourceUnitStyle("\"${source.unit.id}\"")}: ${errorStyle(exception.message ?: "")}"), true)
                }
                is SourceLink.Generated -> {
                    terminal.println(Text("$prefix Failed to compile ${sourceTextStyle("\"${source.text}\"")} ${sourceRefStyle("generated by \"${source.generator}\"")}: ${errorStyle(exception.message ?: "")}"), true)
                }
            }
        }
        is CompilationException -> {
            terminal.println(Text("$prefix Failed to compile code: ${errorStyle(exception.message ?: "")}"), true)
        }
        else -> {
            terminal.println(Text("$prefix Unexpected exception: ${errorStyle(exception.message ?: "")}"), true)
        }
    }
    exception.cause?.let { printCompilationStackTrace(it, terminal, depth + 1) }
}
