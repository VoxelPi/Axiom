package net.voxelpi.axiom.cli.emulator

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.voxelpi.axiom.AxiomBuildParameters
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.Assembler
import net.voxelpi.axiom.cli.command.AxiomCommandManager
import net.voxelpi.axiom.cli.command.AxiomCommandSender
import net.voxelpi.axiom.cli.emulator.command.EmulatorBreakCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorCarryCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorClearCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorExecuteInstructionCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorHistoryCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorInputCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorLoadCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorMemoryCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorPCCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorRegisterCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorResetCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorRunCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorStackCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorStopCommand
import net.voxelpi.axiom.cli.emulator.command.EmulatorVersionCommand
import net.voxelpi.axiom.cli.emulator.computer.EmulatedComputer
import net.voxelpi.axiom.cli.util.ValueFormat
import net.voxelpi.axiom.cli.util.codePointFromString
import net.voxelpi.axiom.cli.util.formattedValue
import net.voxelpi.axiom.cli.util.generateCompilationStackTraceMessage
import net.voxelpi.axiom.cli.util.generateFormattedDescription
import net.voxelpi.axiom.cli.util.stringFromCodePoint
import net.voxelpi.axiom.computer.state.ComputerStatePatch
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.util.parseInteger
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoSuchCommandException
import org.jline.reader.Candidate
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class Emulator(
    val architecture: Architecture,
    initialProgram: String? = null,
) {

    val computer = EmulatedComputer(architecture, ::handleTrace, ::handleInputRequest, ::handleOutput, ::handleWarning)

    val terminal = TerminalBuilder.builder().apply {
        system(true)
    }.build()

    val commandLineReader: LineReader

    init {
        val localHistoryFile = findHistoryFile()
        localHistoryFile.createParentDirectories()

        commandLineReader = LineReaderBuilder.builder().apply {
            appName("Axiom Emulator")
            terminal(terminal)
            completer { reader, line, candidates ->
                val suggestions = commandManager.suggestionFactory().suggestImmediately(AxiomCommandSender(terminal, commandLineReader), line.line())
                candidates += suggestions.list().map { Candidate(it.suggestion()) }
            }
            variable(LineReader.HISTORY_FILE, localHistoryFile.pathString)
            option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            option(LineReader.Option.INSERT_TAB, false)
        }.build()
    }

    val commandManager = AxiomCommandManager().apply {
        registerCommands(EmulatorBreakCommand(computer))
        registerCommands(EmulatorCarryCommand(computer))
        registerCommands(EmulatorClearCommand)
        registerCommands(EmulatorExecuteInstructionCommand(computer))
        registerCommands(EmulatorHistoryCommand(computer))
        registerCommands(EmulatorInputCommand(computer))
        registerCommands(EmulatorLoadCommand(this@Emulator))
        registerCommands(EmulatorMemoryCommand(computer))
        registerCommands(EmulatorPCCommand(computer))
        registerCommands(EmulatorRegisterCommand(computer))
        registerCommands(EmulatorResetCommand(computer))
        registerCommands(EmulatorRunCommand(computer))
        registerCommands(EmulatorStackCommand(computer))
        registerCommands(EmulatorStopCommand(this@Emulator))
        registerCommands(EmulatorVersionCommand())
    }

    private var shouldRun = true

    private var lastInputFilePath: Path? = null

    fun loadProgram(filename: String) {
        val inputFilePath = Path(filename).absolute().normalize()
        lastInputFilePath = inputFilePath

        if (!inputFilePath.exists() || !inputFilePath.isRegularFile()) {
            terminal.writer().println("$PREFIX_EMULATOR The input file $inputFilePath does not exist.")
            return
        }

        val assembler = Assembler(
            listOf(
                Path(".").absolute().normalize(),
            )
        )

        val program: Program = assembler.assemble(inputFilePath, architecture).getOrElse { exception ->
            terminal.writer().println(TextColors.brightRed(TextStyles.bold("COMPILATION FAILED")))
            terminal.writer().println(generateCompilationStackTraceMessage(exception))
            return
        }
        if (computer.isExecuting()) {
            terminal.writer().println(TextColors.brightRed(TextStyles.bold("Failed to load program, because the computer is currently running")))
            return
        }
        computer.load(program).getOrElse {
            terminal.writer().println(TextColors.brightRed(TextStyles.bold("Failed to load program, ${it.message}")))
            return
        }

        terminal.writer().println("$PREFIX_EMULATOR Loaded program \"${inputFilePath.absolutePathString()}\"")
    }

    fun reloadProgram() {
        val path = lastInputFilePath
        if (path == null) {
            terminal.writer().println("$PREFIX_EMULATOR No program was loaded yet.")
            return
        }
        loadProgram(path.absolutePathString())
    }

    init {
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.flush()

        terminal.writer().println(HEADER_MESSAGE)

        if (initialProgram != null) {
            loadProgram(initialProgram)
        }

        while (shouldRun) {
            try {
                val line = commandLineReader.readLine("> ").trim()
                if (line.isNotBlank()) {
                    val lineAsNumber = parseInteger(line)?.toULong()
                    if (lineAsNumber != null) {
                        val value = lineAsNumber and computer.architecture.dataWordType.mask
                        computer.inputQueue.addLast(value)
                        terminal.writer().println("$PREFIX_EMULATOR Added ${TextColors.brightGreen(value.toString())} to the input queue.")
                        continue
                    }

                    if (line.startsWith("'") && line.endsWith("'") && line.length >= 3) {
                        val codePoint = codePointFromString(line.substring(1, line.length - 1))
                        val value = codePoint and computer.architecture.dataWordType.mask
                        computer.inputQueue.addLast(value)
                        terminal.writer().println("$PREFIX_EMULATOR Added ${TextColors.brightGreen(value.toString())} ${TextColors.brightCyan("'")}${TextColors.brightGreen(stringFromCodePoint(value))}${TextColors.brightCyan("'")} to the input queue.")
                        continue
                    }

                    try {
                        runBlocking {
                            commandManager.commandExecutor().executeCommand(AxiomCommandSender(terminal, commandLineReader), line).await()
                        }
                    } catch (exception: NoSuchCommandException) {
                        terminal.writer().println("$PREFIX_EMULATOR $PREFIX_ERROR Unknown command \"${exception.suppliedCommand()}\"")
                        continue
                    } catch (exception: InvalidSyntaxException) {
                        terminal.writer().println("$PREFIX_EMULATOR $PREFIX_ERROR ${exception.message ?: "Invalid command syntax."}")
                        continue
                    } catch (exception: ArgumentParseException) {
                        terminal.writer().println("$PREFIX_EMULATOR $PREFIX_ERROR Failed to parse argument. ${exception.message ?: ""}")
                        continue
                    } catch (exception: Exception) {
                        terminal.writer().println("$PREFIX_EMULATOR $PREFIX_ERROR An error occurred while executing the command: ${exception.message}")
                        continue
                    }
                }
            } catch (_: UserInterruptException) {
                terminal.writer().println("$PREFIX_EMULATOR Stopping the emulator (Interrupted).")
                break
            } catch (_: EndOfFileException) {
                terminal.writer().println("$PREFIX_EMULATOR Stopping the emulator (EOF).")
                break
            }
        }

        computer.stop()
        terminal.close()
    }

    fun stop() {
        shouldRun = false
    }

    private fun handleInputRequest() {
        commandLineReader.printAbove("$PREFIX_COMPUTER The computer is waiting for input.")
    }

    private fun handleOutput(value: ULong) {
        val decimal: String = formattedValue(value, architecture.dataWordType, ValueFormat.DECIMAL)
        val decimalSigned: String = formattedValue(value, architecture.dataWordType, ValueFormat.DECIMAL_SIGNED)
        val hexadecimal: String = formattedValue(value, architecture.dataWordType, ValueFormat.HEXADECIMAL)
        val binary: String = formattedValue(value, architecture.dataWordType, ValueFormat.BINARY)
        val character: String = formattedValue(value, architecture.dataWordType, ValueFormat.CHARACTER)
        commandLineReader.printAbove("$PREFIX_COMPUTER ${TextColors.brightMagenta("[OUTPUT]")}  $decimal  $decimalSigned  $hexadecimal  $binary  $character")
    }

    private fun handleTrace(patch: ComputerStatePatch<*>) {
        commandLineReader.printAbove("$PREFIX_COMPUTER ${TextColors.brightCyan("[TRACE]")}  ${generateFormattedDescription(patch, computer.architecture)}")
    }

    private fun handleWarning(message: String) {
        commandLineReader.printAbove("$PREFIX_COMPUTER ${TextColors.brightYellow("[WARNING]")}  $message")
    }

    private fun findHistoryFile(): Path {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> {
                val appDataDir = Path(System.getenv("APPDATA") ?: System.getProperty("user.home") ?: ".")
                appDataDir / "Axiom" / "command_history.txt"
            }
            else -> {
                val localDataHome = Path(System.getenv("XDG_STATE_HOME") ?: "${System.getProperty("user.home") ?: "."}/.local/state")
                localDataHome / "axiom" / "command_history.txt"
            }
        }.absolute().normalize()
    }

    companion object {
        val PREFIX_EMULATOR = (TextStyles.bold + TextColors.brightMagenta)("[EMULATOR]")
        val PREFIX_COMPUTER = (TextStyles.bold + TextColors.brightBlue)("[COMPUTER]")
        val PREFIX_ERROR = (TextStyles.bold + TextColors.brightRed)("[ERROR]")

        val HEADER_MESSAGE = """                                                                               
             _____ __ __ _____ _____ _____    _____ _____ _____ __    _____ _____ _____ _____ 
            |  _  |  |  |     |     |     |  |   __|     |  |  |  |  |  _  |_   _|     | __  |   ${TextColors.rgb("#98C379")("Version")}: ${AxiomBuildParameters.VERSION}
            |     |-   -|-   -|  |  | | | |  |   __| | | |  |  |  |__|     | | | |  |  |    -|   ${TextColors.rgb("#98C379")("Branch")}: ${AxiomBuildParameters.GIT_BRANCH}
            |__|__|__|__|_____|_____|_|_|_|  |_____|_|_|_|_____|_____|__|__| |_| |_____|__|__|   ${TextColors.rgb("#98C379")("Commit")}: ${AxiomBuildParameters.GIT_COMMIT.substring(0..6)}
                                                                                              
        """.trimIndent()
    }
}
