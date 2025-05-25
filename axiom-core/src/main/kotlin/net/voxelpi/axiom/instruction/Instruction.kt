package net.voxelpi.axiom.instruction

import net.voxelpi.axiom.Register
import net.voxelpi.axiom.ValueProvider

public data class Instruction(
    val operation: Operation,
    val condition: Condition,
    val conditionRegister: Register,
    val outputRegister: Register,
    val inputA: ValueProvider,
    val inputB: ValueProvider,
)
