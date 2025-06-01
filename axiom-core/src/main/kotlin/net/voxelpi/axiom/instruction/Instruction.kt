package net.voxelpi.axiom.instruction

import net.voxelpi.axiom.register.RegisterVariable

public data class Instruction(
    val operation: Operation,
    val condition: Condition,
    val conditionRegister: RegisterVariable,
    val outputRegister: RegisterVariable,
    val inputA: InstructionValue,
    val inputB: InstructionValue,
) {
    init {
        if (inputA is InstructionValue.RegisterReference) {
            require(inputA.register.readable) { "The register variable '${inputA.register.id}' cannot be read." }
        }
        if (inputB is InstructionValue.RegisterReference) {
            require(inputB.register.readable) { "The register variable '${inputB.register.id}' cannot be read." }
        }
        require(conditionRegister.conditionable) { "The register variable '${conditionRegister.id}' cannot be used as condition value." }
        require(outputRegister.writable) { "The register variable '${outputRegister.id}' cannot be written to.'" }
    }

    override fun toString(): String {
        return when (condition) {
            Condition.ALWAYS -> operation.asString(outputRegister, inputA, inputB)
            Condition.NEVER -> "nop"
            else -> {
                "${operation.asString(outputRegister, inputA, inputB)} if $conditionRegister ${condition.symbol} 0"
            }
        }
    }
}
