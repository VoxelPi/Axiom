package net.voxelpi.axiom.register

import net.voxelpi.axiom.WordType

public sealed interface RegisterVariable<V : Comparable<V>, R : Comparable<R>> {

    public val id: String

    public val type: WordType<V>

    public val register: Register<R>

    public val address: Int

    public val readable: Boolean

    public val writable: Boolean

    public val conditionable: Boolean

    public data class Direct<V : Comparable<V>>(
        override val id: String,
        override val register: Register<V>,
        override val address: Int,
        override val readable: Boolean,
        override val writable: Boolean,
        override val conditionable: Boolean,
    ) : RegisterVariable<V, V> {

        override val type: WordType<V>
            get() = register.type
    }

    public data class Part<V : Comparable<V>, R : Comparable<R>>(
        override val id: String,
        override val type: WordType<V>,
        override val register: Register<R>,
        val part: Int,
        override val address: Int,
        override val readable: Boolean,
        override val writable: Boolean,
        override val conditionable: Boolean,
    ) : RegisterVariable<V, R> {

        init {
            require(type.bits * part < register.type.bits)
        }
    }
}
