package net.voxelpi.axiom.asm.compositor.token

import kotlin.reflect.KClass

internal sealed interface CompositorDirectiveParameterValue<T : Any> {

    val type: KClass<T>

    data class Value<T : Any>(
        override val type: KClass<T>,
        val value: T,
    ) : CompositorDirectiveParameterValue<T>

    data class Placeholder<T : Any>(
        override val type: KClass<T>,
    ) : CompositorDirectiveParameterValue<T>
}
