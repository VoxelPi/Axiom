package net.voxelpi.axiom

public data class Register(
    val id: String,
) : ValueProvider {

    override fun toString(): String {
        return id
    }
}
