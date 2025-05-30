package net.voxelpi.axiom.cli.emulator

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandSender
import net.voxelpi.axiom.cli.emulator.command.EmulatorStopCommand
import net.voxelpi.axiom.emulator.EmulatedComputer
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

    val computer = EmulatedComputer(architecture, program, ::handleInputPoll, ::provideInput, ::handleOutput)

    val terminal = TerminalBuilder.builder().apply {
        system(true)
    }.build()

    val commandLineReader = LineReaderBuilder.builder().apply {
        appName("Axiom Emulator")
        terminal(terminal)
        completer { reader, line, candidates ->
            val suggestions = commandManager.suggestionFactory().suggestImmediately(AxiomCommandSender, line.line())
            candidates += suggestions.list().map { Candidate(it.suggestion()) }
        }
        option(LineReader.Option.DISABLE_EVENT_EXPANSION, false)
        option(LineReader.Option.INSERT_TAB, false)
    }.build()

    val commandManager = AxiomCommandManager().apply {
        registerCommands(EmulatorStopCommand(this@Emulator))
    }

    private var shouldRun = true

    fun start() {
        while (shouldRun) {
            try {
                val line = commandLineReader.readLine("> ").trim()
                if (line.isNotBlank()) {
                    try {
                        runBlocking {
                            commandManager.commandExecutor().executeCommand(AxiomCommandSender, line).await()
                        }
                    } catch (exception: NoSuchCommandException) {
                        terminal.writer().println(exception.message ?: "Unknown command.")
                        continue
                    } catch (exception: InvalidSyntaxException) {
                        terminal.writer().println(exception.message ?: "Invalid syntax.")
                        continue
                    } catch (exception: ArgumentParseException) {
                        terminal.writer().println(exception.message ?: "Failed to parse argument.")
                        continue
                    } catch (exception: Exception) {
                        terminal.writer().println("An error occurred while executing the command: ${exception.message}")
                        continue
                    }
                }
            } catch (_: UserInterruptException) {
                terminal.writer().println("Interrupted.")
                break
            } catch (_: EndOfFileException) {
                terminal.writer().println("EOF")
                break
            }
        }
    }

    fun stop() {
        shouldRun = false
    }

    private fun handleInputPoll(): Boolean {
        return true
    }

    private fun provideInput(): ULong {
        return 0UL
    }

    private fun handleOutput(value: ULong) {
        terminal.writer().println(value.toString(16).padStart(architecture.dataWordType.bits / 4, '0').uppercase())
    }
}
