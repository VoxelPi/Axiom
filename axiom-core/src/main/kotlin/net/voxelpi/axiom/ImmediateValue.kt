package net.voxelpi.axiom

public data class ImmediateValue(
    val value: Long,
) : ValueProvider {

    override fun toString(): String {
        return value.toString()
    }
}
