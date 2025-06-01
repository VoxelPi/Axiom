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
import kotlinx.cli.optional
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.arch.ax08.AX08Architecture
import net.voxelpi.axiom.arch.dev08.DEV08Architecture
import net.voxelpi.axiom.arch.dev16.DEV16Architecture
import net.voxelpi.axiom.arch.dev32.DEV32Architecture
import net.voxelpi.axiom.arch.dev64.DEV64Architecture
import net.voxelpi.axiom.arch.mcpc08.MCPC08Architecture
import net.voxelpi.axiom.arch.mcpc16.MCPC16Architecture
import net.voxelpi.axiom.asm.Assembler
import net.voxelpi.axiom.cli.emulator.Emulator
import net.voxelpi.axiom.cli.util.generateCompilationStackTraceMessage
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
        DEV08Architecture,
        DEV16Architecture,
        DEV32Architecture,
        DEV64Architecture,
        MCPC08Architecture,
        MCPC16Architecture,
    ).associateBy(Architecture<*, *>::id)

    class Assemble : Subcommand("assemble", "Assembles a program.") {
        val input by argument(ArgType.String, description = "The input file.")
        val output by option(ArgType.String, "output", "o", description = "The output file.")
        val position by option(ArgType.Int, "position", "p", description = "The position of the program.").default(0)
        val generateRawAxm by option(ArgType.Boolean, "raw", "r", description = "If a raw axm file should be generated").default(false)
        val inverseInstructionByteOrder by option(ArgType.Boolean, "inverse", "i", description = "If the instruction byte order should be inverted").default(false)

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

            val assembler = Assembler(listOf(Path(".").absolute().normalize()))

            val program = assembler.assemble(inputFilePath, architecture, offset = position).getOrElse { exception ->
                terminal.println(Text(TextColors.brightRed(TextStyles.bold("COMPILATION FAILED"))), true)
                terminal.println(Text(generateCompilationStackTraceMessage(exception)), true)
                exitProcess(1)
            }

            if (generateRawAxm) {
                rawFile.writeText(program.toString())
            }

            if (architecture.hasEncodedFormat) {
                val encodedProgram = architecture.encodeProgram(program, invertByteOrder = inverseInstructionByteOrder).getOrThrow()
                outputFilePath.writeBytes(encodedProgram.toByteArray())
                println("Assembled ${program.instructions.size} instructions (${encodedProgram.size} bytes)")
            } else {
                println("Assembled ${program.instructions.size} instructions")
            }
        }
    }

    class Emulate() : Subcommand("emulate", "Emulates a program.") {
        val program by argument(ArgType.String, description = "The program file.").optional()

        val architecture by option(
            ArgType.Choice(choices = architectures.values.toList(), toVariant = { architectures[it]!! }),
            fullName = "architecture",
            shortName = "a",
            description = "The architecture to use."
        ).default(AX08Architecture)

        override fun execute() {
            Emulator(architecture, program)
        }
    }

    parser.subcommands(Assemble(), Emulate())
    parser.parse(args)
}
