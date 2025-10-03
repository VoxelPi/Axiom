package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.language.NamespacedId
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.SourcedValue
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

internal sealed interface DirectiveParameterValue<T : Any> {

    val type: KClass<T>

    data class Value<T : Any>(
        override val type: KClass<T>,
        val value: SourcedValue<T>,
    ) : DirectiveParameterValue<T> {

        companion object {
            inline fun <reified T : Any> of(value: T, source: SourceReference): Value<T> {
                return Value(T::class, SourcedValue(value, typeOf<T>(), source))
            }
        }
    }

    data class Placeholder<T : Any>(
        override val type: KClass<T>,
        val id: SourcedValue<NamespacedId>,
    ) : DirectiveParameterValue<T>
}
