package net.voxelpi.axiom.cli.emulator.computer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.computer.Computer
import net.voxelpi.axiom.computer.state.ComputerState
import net.voxelpi.axiom.instruction.Program
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class EmulatedComputer(
    architecture: Architecture<*, *>,
    program: Program,
    inputAvailableProvider: () -> Boolean,
    inputProvider: () -> ULong,
    outputHandler: (ULong) -> Unit,
) : CoroutineScope {

    private val coroutineExecutor = ComputerExecutor()
    override val coroutineContext: CoroutineContext = SupervisorJob() + coroutineExecutor.asCoroutineDispatcher()

    val computer = Computer(architecture, program, inputAvailableProvider, inputProvider, outputHandler)

    val architecture: Architecture<*, *>
        get() = computer.architecture

    private var nExecutedInstructions: Int = 0
    private var remainingInstructions: Int = 0
    private var doneCallback: (Int) -> Unit = {}

    val computerThread = thread(start = true, name = "Axiom Emulator Computer Thread", isDaemon = true) {
        try {
            while (true) {
                if (remainingInstructions > 0) {
                    // Run instruction
                    val result = computer.runSingleInstruction()
                    ++nExecutedInstructions

                    if (result.hitBreak) {
                        remainingInstructions = 0
                    } else {
                        --remainingInstructions
                    }

                    if (remainingInstructions == 0) {
                        doneCallback.invoke(nExecutedInstructions)
                        doneCallback = {}
                    }
                }

                coroutineExecutor.runTasks()

                Thread.sleep(1)
            }
        } catch (_: InterruptedException) {
        }
    }

    suspend fun state(): ComputerState<*> {
        return withContext(coroutineContext) {
            computer.currentState()
        }
    }

    fun run(nInstructions: Int = Int.MAX_VALUE, callback: (Int) -> Unit) {
        if (isExecuting()) {
            throw IllegalStateException("The computer is already running.")
        }
        doneCallback = callback
        nExecutedInstructions = 0
        remainingInstructions = nInstructions
    }

    fun isExecuting(): Boolean {
        return remainingInstructions > 0
    }

    fun stop() {
        computerThread.interrupt()
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
