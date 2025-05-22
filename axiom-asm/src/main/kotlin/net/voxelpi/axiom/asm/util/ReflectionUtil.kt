package net.voxelpi.axiom.asm.util

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

internal fun isInstanceOfType(value: Any?, type: KType): Boolean {
    // Handle null values: only return true if 'value' is nullable
    if (value == null) return type.isMarkedNullable

    return when (val classifier = type.classifier) {
        is KClass<*> -> classifier.isSuperclassOf(value::class)
        else -> classifier == value::class
    }
}
