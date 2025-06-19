package net.voxelpi.axiom.instruction

/**
 * A constant value that is embedded directly into the program memory.
 */
public data class ProgramConstant(
    val value: ULong,
    override val meta: Map<String, Any?>,
) : ProgramElement {

    override fun toString(): String {
        return value.toString()
    }
}
