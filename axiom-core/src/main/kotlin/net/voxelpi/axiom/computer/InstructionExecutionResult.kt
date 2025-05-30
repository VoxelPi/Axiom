package net.voxelpi.axiom.computer

import net.voxelpi.axiom.computer.state.ComputerStateChange

public data class InstructionExecutionResult(
    val changes: List<ComputerStateChange>,
    val conditionMet: Boolean,
    val hitBreak: Boolean,
)
