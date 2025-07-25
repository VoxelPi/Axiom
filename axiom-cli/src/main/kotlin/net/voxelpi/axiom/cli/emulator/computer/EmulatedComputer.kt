package net.voxelpi.axiom.cli.emulator.computer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.Computer
import net.voxelpi.axiom.computer.state.ComputerState
import net.voxelpi.axiom.computer.state.ComputerStatePatch
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.instruction.ProgramConstant
import net.voxelpi.axiom.instruction.ProgramElement
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class EmulatedComputer(
    architecture: Architecture,
    val traceHandler: (patch: ComputerStatePatch<*>) -> Unit,
    val inputRequestHandler: () -> Unit,
    val outputHandler: (ULong) -> Unit,
    val warningHandler: (String) -> Unit,
) : CoroutineScope {

    private val coroutineExecutor = ComputerExecutor()
    override val coroutineContext: CoroutineContext = SupervisorJob() + coroutineExecutor.asCoroutineDispatcher()

    val computer = Computer(architecture, ::handleInputPoll, ::provideInput, ::handleOutput, warningHandler)

    val architecture: Architecture
        get() = computer.architecture

    private var nExecutedInstructions: Int = 0
    private var remainingInstructions: Int = 0
    private var doneCallback: (Int) -> Unit = {}

    val inputQueue: ArrayDeque<ULong> = ArrayDeque()
    private var shouldHalt = false
    private var undoCurrentInstruction = false
    var trace = false
        private set
    var silent = false
        private set

    val computerThread = thread(start = true, name = "Axiom Emulator Computer Thread", isDaemon = true) {
        try {
            while (true) {
                if (shouldHalt) {
                    remainingInstructions = 0
                    doneCallback.invoke(nExecutedInstructions)
                    doneCallback = {}
                    trace = false
                    silent = false
                    shouldHalt = false
                }

                if (remainingInstructions > 0) {

                    // Run instruction
                    val result = computer.runSingleInstruction()
                    if (undoCurrentInstruction) {
                        computer.stepBackwards()
                        computer.eraseFuture()
                        undoCurrentInstruction = false
                    } else {
                        if (trace) {
                            traceHandler.invoke(result)
                        }
                        ++nExecutedInstructions

                        if (result.reason.hitBreak) {
                            remainingInstructions = 0
                        } else {
                            --remainingInstructions
                        }
                    }

                    if (remainingInstructions == 0) {
                        doneCallback.invoke(nExecutedInstructions)
                        doneCallback = {}
                        trace = false
                        silent = false
                    }
                }

                coroutineExecutor.runTasks()

                Thread.sleep(1)
            }
        } catch (_: InterruptedException) {
        }
    }

    suspend fun state(): ComputerState {
        return withContext(coroutineContext) {
            computer.currentState()
        }
    }

    suspend fun modifyState(block: ComputerStatePatch.Builder.() -> Unit): ComputerState {
        return withContext(coroutineContext) {
            computer.modifyState(block)
        }
    }

    suspend fun runInlineInstructions(program: Program, trace: Boolean = false, silent: Boolean = false): Int {
        return withContext(coroutineContext) {
            var executedInstructions: Int = 0

            val previousSilent = this@EmulatedComputer.silent
            this@EmulatedComputer.silent = silent
            for (programElement in program.data) {
                val instruction = when (programElement) {
                    is Instruction -> programElement
                    is ProgramConstant -> {
                        warningHandler("Executing program constant.")
                        val bytes = architecture.instructionWordType.pack(programElement.value)
                        architecture.decodeInstruction(bytes).getOrThrow()
                    }
                    is ProgramElement.None -> {
                        break
                    }
                }

                val patch = computer.runInlineInstruction(instruction)
                executedInstructions += 1
                if (trace) {
                    traceHandler.invoke(patch)
                }
            }
            this@EmulatedComputer.silent = previousSilent

            executedInstructions
        }
    }

    fun load(program: Program): Result<Unit> {
        if (isExecuting()) {
            return Result.failure(IllegalStateException("The computer is already running."))
        }
        computer.loadProgram(program)
        return Result.success(Unit)
    }

    fun run(nInstructions: Int = Int.MAX_VALUE, trace: Boolean = false, silent: Boolean = false, callback: (Int) -> Unit): Result<Unit> {
        if (isExecuting()) {
            return Result.failure(IllegalStateException("The computer is already running."))
        }
        if (nInstructions <= 0) {
            return Result.success(Unit)
        }
        this.trace = trace
        this.silent = silent
        doneCallback = callback
        nExecutedInstructions = 0
        remainingInstructions = nInstructions
        return Result.success(Unit)
    }

    fun halt() {
        shouldHalt = true
    }

    fun reset() {
        if (isExecuting()) {
            return
        }
        inputQueue.clear()
        computer.reset()
    }

    fun isExecuting(): Boolean {
        return remainingInstructions > 0
    }

    fun stop() {
        computerThread.interrupt()
    }

    fun stepBackwardsWithTrace(): ComputerStatePatch<*>? {
        if (isExecuting()) {
            return null
        }
        val patch = computer.stepBackwards() ?: return null
        traceHandler.invoke(patch)
        return patch
    }

    fun stepForwardsWithTrace(): ComputerStatePatch<*>? {
        if (isExecuting()) {
            return null
        }
        val patch = computer.stepForwards() ?: return null
        traceHandler.invoke(patch)
        return patch
    }

    private fun handleInputPoll(): Boolean {
        return inputQueue.isNotEmpty()
    }

    private fun provideInput(): ULong {
        val value = inputQueue.removeFirstOrNull()
        if (value != null) {
            return value
        }

        inputRequestHandler.invoke()
        while (true) {
            val value = inputQueue.removeFirstOrNull()
            if (value != null) {
                return value
            }
            if (shouldHalt) {
                undoCurrentInstruction = true
                return 0UL
            }

            coroutineExecutor.runTasks()
            Thread.sleep(1)
        }
    }

    private fun handleOutput(value: ULong) {
        if (!silent) {
            outputHandler.invoke(value)
        }
    }

    private class ComputerExecutor : Executor {
        private val taskQueue: BlockingQueue<Runnable> = LinkedBlockingQueue()

        override fun execute(command: Runnable) {
            taskQueue.offer(command)
        }

        fun runTasks() {
            while (true) {
                val task = taskQueue.poll() ?: break
                task.run()
            }
        }
    }
}
