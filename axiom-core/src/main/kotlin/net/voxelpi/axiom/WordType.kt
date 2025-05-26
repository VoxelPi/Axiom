package net.voxelpi.axiom

import kotlin.reflect.KClass

public sealed class WordType<T : Comparable<T>>(
    public val bits: Int,
    public val type: KClass<T>,
) : Comparable<WordType<*>> {

    override fun compareTo(other: WordType<*>): Int {
        return bits.compareTo(other.bits)
    }

    public data object INT8 : WordType<UByte>(8, UByte::class)

    public data object INT16 : WordType<UShort>(16, UShort::class)

    public data object INT32 : WordType<UInt>(32, UInt::class)

    public data object INT64 : WordType<ULong>(64, ULong::class)
}
