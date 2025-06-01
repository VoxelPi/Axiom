package net.voxelpi.axiom

/**
 * The type of data word.
 * @property bytes the number of bytes the word type uses.
 */
public sealed class WordType(
    public val bytes: Int,
) : Comparable<WordType> {

    /**
     * The number of bits of the word type.
     */
    public val bits: Int
        get() = bytes * 8

    /**
     * The mask that should be applied to convert any uint64 to this word type.
     */
    public abstract val mask: ULong

    override fun compareTo(other: WordType): Int {
        return bytes.compareTo(other.bytes)
    }

    public abstract fun pack(value: ULong): UByteArray

    public abstract fun unpack(value: UByteArray): ULong

    public fun unsignedValueOf(value: ULong): ULong {
        return value and mask
    }

    public fun signedValueOf(value: ULong): Long {
        val isNegative = value and (1uL shl (bits - 1)) != 0uL
        return ((value and mask) or (if (isNegative) mask.inv() else 0uL)).toLong()
    }

    public data object INT8 : WordType(1) {

        override val mask: ULong
            get() = UByte.MAX_VALUE.toULong()

        override fun pack(value: ULong): UByteArray {
            return ubyteArrayOf(
                ((value shr 0) and 0xFFu).toUByte(),
            )
        }

        override fun unpack(value: UByteArray): ULong {
            require(value.size == 1) { "Expected exactly 1 byte, got ${value.size}." }
            return value[0].toULong()
        }
    }

    public data object INT16 : WordType(2) {

        override val mask: ULong
            get() = UShort.MAX_VALUE.toULong()

        override fun pack(value: ULong): UByteArray {
            return ubyteArrayOf(
                ((value shr 0) and 0xFFu).toUByte(),
                ((value shr 8) and 0xFFu).toUByte(),
            )
        }

        override fun unpack(value: UByteArray): ULong {
            require(value.size == 2) { "Expected exactly 2 bytes, got ${value.size}." }
            return (value[0].toULong() shl 0) or (value[1].toULong() shl 8)
        }
    }

    public data object INT32 : WordType(4) {

        override val mask: ULong
            get() = UInt.MAX_VALUE.toULong()

        override fun pack(value: ULong): UByteArray {
            return ubyteArrayOf(
                ((value shr 0) and 0xFFu).toUByte(),
                ((value shr 8) and 0xFFu).toUByte(),
                ((value shr 16) and 0xFFu).toUByte(),
                ((value shr 24) and 0xFFu).toUByte(),
            )
        }

        override fun unpack(value: UByteArray): ULong {
            require(value.size == 4) { "Expected exactly 4 bytes, got ${value.size}." }
            var result = 0UL
            result = result or (value[0].toULong() shl 0)
            result = result or (value[1].toULong() shl 8)
            result = result or (value[2].toULong() shl 16)
            result = result or (value[3].toULong() shl 24)
            return result
        }
    }

    public data object INT64 : WordType(8) {

        override val mask: ULong
            get() = ULong.MAX_VALUE

        override fun pack(value: ULong): UByteArray {
            return ubyteArrayOf(
                ((value shr 0) and 0xFFu).toUByte(),
                ((value shr 8) and 0xFFu).toUByte(),
                ((value shr 16) and 0xFFu).toUByte(),
                ((value shr 24) and 0xFFu).toUByte(),
                ((value shr 32) and 0xFFu).toUByte(),
                ((value shr 40) and 0xFFu).toUByte(),
                ((value shr 48) and 0xFFu).toUByte(),
                ((value shr 56) and 0xFFu).toUByte(),
            )
        }

        override fun unpack(value: UByteArray): ULong {
            require(value.size == 8) { "Expected exactly 8 bytes, got ${value.size}." }
            var result: ULong = 0u
            result = result or (value[0].toULong() shl 0)
            result = result or (value[1].toULong() shl 8)
            result = result or (value[2].toULong() shl 16)
            result = result or (value[3].toULong() shl 24)
            result = result or (value[0].toULong() shl 32)
            result = result or (value[1].toULong() shl 40)
            result = result or (value[2].toULong() shl 48)
            result = result or (value[3].toULong() shl 56)
            return result
        }
    }
}
