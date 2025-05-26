package net.voxelpi.axiom.instruction

public data class Program(
    val instructions: List<Instruction>,
) {

    override fun toString(): String {
        return instructions.joinToString("\n")
    }
}
