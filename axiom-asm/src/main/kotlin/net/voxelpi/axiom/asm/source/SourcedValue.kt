package net.voxelpi.axiom.asm.source

import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class SourcedValue<T>(
    val value: T,
    val type: KType,
    override val source: SourceReference,
) : Sourced

public inline fun <reified T> sourcedValue(value: T, source: SourceReference): SourcedValue<T> {
    return SourcedValue(value, typeOf<T>(), source)
}
