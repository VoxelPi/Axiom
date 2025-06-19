package net.voxelpi.axiom.instruction

public data class Program(
    val data: List<ProgramElement>,
) {

    override fun toString(): String {
        return data.joinToString("\n")
    }
}
