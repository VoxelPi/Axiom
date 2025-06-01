package net.voxelpi.axiom.register

import net.voxelpi.axiom.WordType

public data class Register(
    val id: String,
    val type: WordType,
) {

    override fun toString(): String {
        return id
    }
}
