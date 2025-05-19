package net.voxelpi.axiom.instruction

public data class Instruction(
    val operation: Operation,
    val condition: Condition,
    val conditionRegister: Register,
    val outputRegister: Register,
    val inputA: InstructionDataSource,
    val inputB: InstructionDataSource,
)
