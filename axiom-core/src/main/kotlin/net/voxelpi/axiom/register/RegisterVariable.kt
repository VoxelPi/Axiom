package net.voxelpi.axiom.register

import net.voxelpi.axiom.WordType

public sealed interface RegisterVariable {

    public val id: String

    public val type: WordType

    public val register: Register

    public val address: Int

    public val readable: Boolean

    public val writable: Boolean

    public val needsMode2: Boolean

    public val conditionable: Boolean

    public val mask: ULong?

    public data class Direct(
        override val id: String,
        override val register: Register,
        override val address: Int,
        override val readable: Boolean,
        override val writable: Boolean,
        override val conditionable: Boolean,
        override val needsMode2: Boolean,
        override val mask: ULong?,
    ) : RegisterVariable {

        override val type: WordType
            get() = register.type

        override fun toString(): String {
            return id
        }
    }

    public data class Part(
        override val id: String,
        override val type: WordType,
        override val register: Register,
        val part: Int,
        override val address: Int,
        override val readable: Boolean,
        override val writable: Boolean,
        override val conditionable: Boolean,
        override val needsMode2: Boolean,
        override val mask: ULong?,
    ) : RegisterVariable {

        init {
            require(type.bits * part < register.type.bits)
        }

        override fun toString(): String {
            return id
        }
    }
}
