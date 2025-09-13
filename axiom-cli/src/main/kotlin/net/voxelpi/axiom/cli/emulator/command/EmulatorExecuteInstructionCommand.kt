package net.voxelpi.axiom.cli.emulator.command

import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandProvider
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.StringParser

class EmulatorExecuteInstructionCommand(
    private val computer: EmulatedComputer,
) : AxiomCommandProvider {

    override fun registerCommands(commandManager: AxiomCommandManager) {
        commandManager.buildAndRegister("execute") {
            required("instruction", StringParser.greedyFlagYieldingStringParser())
            flag("trace", aliases = arrayOf("t"))
            flag("silent", aliases = arrayOf("s"))

            handler { context ->
                val source: String = context["instruction"]
                val trace = context.flags().isPresent("trace")
                val silent = context.flags().isPresent("silent")

                // TODO: Assembler v2
                // // Assemble instructions.
                // val assembler = Assembler(emptyList())
                // val program = assembler.assemble(source, computer.architecture, Parsers.INLINE_ASM).getOrElse { exception ->
                //     context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} ${Emulator.PREFIX_ERROR} Failed to parse instruction ${TextColors.brightCyan("\"${source.trim()}\"")}")
                //     context.sender().terminal.writer().println(generateCompilationStackTraceMessage(exception))
                //     return@handler
                // }
                //
                // // Run instructions.
                // val executedInstructions = runBlocking {
                //     computer.runInlineInstructions(program, trace = trace, silent = silent)
                // }
                // context.sender().terminal.writer().println("${Emulator.PREFIX_EMULATOR} Executed ${TextColors.brightYellow(executedInstructions.toString())} instructions")
            }
        }
    }
}
