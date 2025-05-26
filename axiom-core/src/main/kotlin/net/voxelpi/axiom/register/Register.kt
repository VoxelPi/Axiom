package net.voxelpi.axiom.register

import net.voxelpi.axiom.WordType

public data class Register<T : Comparable<T>>(
    val id: String,
    val type: WordType<T>,
) {

    override fun toString(): String {
        return id
    }
}
