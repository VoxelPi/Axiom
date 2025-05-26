package net.voxelpi.axiom

import kotlin.reflect.KClass

public sealed class WordType<T : Comparable<T>>(
    public val bytes: Int,
    public val type: KClass<T>,
) : Comparable<WordType<*>> {

    public val bits: Int
        get() = bytes * 8

    override fun compareTo(other: WordType<*>): Int {
        return bytes.compareTo(other.bytes)
    }

    public abstract fun pack(value: T): UByteArray

    public abstract fun unpack(value: UByteArray): T

    public data object INT8 : WordType<UByte>(1, UByte::class) {

        override fun pack(value: UByte): UByteArray {
            return ubyteArrayOf(value)
        }

        override fun unpack(value: UByteArray): UByte {
            require(value.size == 1) { "Expected exactly 1 byte, got ${value.size}." }
            return value[0]
        }
    }

    public data object INT16 : WordType<UShort>(2, UShort::class) {

        override fun pack(value: UShort): UByteArray {
            return ubyteArrayOf(
                ((value.toUInt() shr 0) and 0xFFu).toUByte(),
                ((value.toUInt() shr 8) and 0xFFu).toUByte(),
            )
        }

        override fun unpack(value: UByteArray): UShort {
            require(value.size == 2) { "Expected exactly 2 bytes, got ${value.size}." }
            return ((value[0].toUInt() shl 0) or (value[1].toUInt() shl 8)).toUShort()
        }
    }

    public data object INT32 : WordType<UInt>(4, UInt::class) {

        override fun pack(value: UInt): UByteArray {
            return ubyteArrayOf(
                ((value shr 0) and 0xFFu).toUByte(),
                ((value shr 8) and 0xFFu).toUByte(),
                ((value shr 16) and 0xFFu).toUByte(),
                ((value shr 24) and 0xFFu).toUByte(),
            )
        }

        override fun unpack(value: UByteArray): UInt {
            require(value.size == 4) { "Expected exactly 4 bytes, got ${value.size}." }
            var result: UInt = 0u
            result = result or (value[0].toUInt() shl 0)
            result = result or (value[1].toUInt() shl 8)
            result = result or (value[2].toUInt() shl 16)
            result = result or (value[3].toUInt() shl 24)
            return result
        }
    }

    public data object INT64 : WordType<ULong>(8, ULong::class) {

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
