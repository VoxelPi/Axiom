package net.voxelpi.axiom.cli.emulator

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.AxiomBuildParameters
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandSender
import net.voxelpi.axiom.cli.emulator.command.EmulatorClearCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorInputCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorRegisterCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorRunCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorStopCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorVersionCommand
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.instruction.Program
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoSuchCommandException
import org.jline.reader.Candidate
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder

class Emulator(
    val architecture: Architecture<*, *>,
    val program: Program,
) {

    private var inputQueue: ArrayDeque<ULong> = ArrayDeque()

    val computer = EmulatedComputer(architecture, program, ::handleInputPoll, ::provideInput, ::handleOutput)

    val terminal = TerminalBuilder.builder().apply {
        system(true)
    }.build()

    val commandLineReader: LineReader = LineReaderBuilder.builder().apply {
        appName("Axiom Emulator")
        terminal(terminal)
        completer { reader, line, candidates ->
            val suggestions = commandManager.suggestionFactory().suggestImmediately(AxiomCommandSender(terminal, commandLineReader), line.line())
            candidates += suggestions.list().map { Candidate(it.suggestion()) }
        }
        option(LineReader.Option.DISABLE_EVENT_EXPANSION, false)
        option(LineReader.Option.INSERT_TAB, false)
    }.build()

    val commandManager = AxiomCommandManager().apply {
        registerCommands(EmulatorClearCommand)
        registerCommands(EmulatorInputCommand(inputQueue))
        registerCommands(EmulatorRegisterCommand(computer))
        registerCommands(EmulatorRunCommand(computer))
        registerCommands(EmulatorStopCommand(this@Emulator))
        registerCommands(EmulatorVersionCommand())
    }

    private var shouldRun = true

    init {
        terminal.writer().println(HEADER_MESSAGE)

        while (shouldRun) {
            try {
                val line = commandLineReader.readLine("> ").trim()
                if (line.isNotBlank()) {
                    try {
                        runBlocking {
                            commandManager.commandExecutor().executeCommand(AxiomCommandSender(terminal, commandLineReader), line).await()
                        }
                    } catch (exception: NoSuchCommandException) {
                        terminal.writer().println(exception.message ?: "$PREFIX_EMULATOR Unknown command.")
                        continue
                    } catch (exception: InvalidSyntaxException) {
                        terminal.writer().println(exception.message ?: "$PREFIX_EMULATOR Invalid syntax.")
                        continue
                    } catch (exception: ArgumentParseException) {
                        terminal.writer().println(exception.message ?: "$PREFIX_EMULATOR Failed to parse argument.")
                        continue
                    } catch (exception: Exception) {
                        terminal.writer().println("$PREFIX_EMULATOR An error occurred while executing the command: ${exception.message}")
                        continue
                    }
                }
            } catch (_: UserInterruptException) {
                terminal.writer().println("$PREFIX_EMULATOR Interrupted.")
                break
            } catch (_: EndOfFileException) {
                terminal.writer().println("$PREFIX_EMULATOR EOF")
                break
            }
        }

        computer.stop()
        terminal.close()
    }

    fun stop() {
        shouldRun = false
    }

    private fun handleInputPoll(): Boolean {
        return inputQueue.isNotEmpty()
    }

    private fun provideInput(): ULong {
        val value = inputQueue.removeFirstOrNull()
        if (value != null) {
            commandLineReader.printAbove("$PREFIX_COMPUTER The computer has consumed input.")
            return value
        }

        commandLineReader.printAbove("$PREFIX_COMPUTER The computer is waiting for input.")
        while (true) {
            val value = inputQueue.removeFirstOrNull()
            if (value != null) {
                commandLineReader.printAbove("$PREFIX_COMPUTER The computer has consumed an value from the input queue.")
                return value
            }
            Thread.sleep(1)
        }
    }

    private fun handleOutput(value: ULong) {
        commandLineReader.printAbove("$PREFIX_COMPUTER output: ${TextColors.brightGreen(value.toString())}")
    }

    companion object {
        val PREFIX_EMULATOR = (TextStyles.bold + TextColors.brightMagenta)("[EMULATOR]")
        val PREFIX_COMPUTER = (TextStyles.bold + TextColors.brightBlue)("[COMPUTER]")

        val HEADER_MESSAGE = """                                                                               
             _____ __ __ _____ _____ _____    _____ _____ _____ __    _____ _____ _____ _____ 
            |  _  |  |  |     |     |     |  |   __|     |  |  |  |  |  _  |_   _|     | __  |   ${TextColors.rgb("#98C379")("Version")}: ${AxiomBuildParameters.VERSION}
            |     |-   -|-   -|  |  | | | |  |   __| | | |  |  |  |__|     | | | |  |  |    -|   ${TextColors.rgb("#98C379")("Branch")}: ${AxiomBuildParameters.GIT_BRANCH}
            |__|__|__|__|_____|_____|_|_|_|  |_____|_|_|_|_____|_____|__|__| |_| |_____|__|__|   ${TextColors.rgb("#98C379")("Commit")}: ${AxiomBuildParameters.GIT_COMMIT.substring(0..6)}
                                                                                              
        """.trimIndent()
    }
}
