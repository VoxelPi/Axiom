package net.voxelpi.axiom.asm.statement

import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class StatementParameter<T>(
    val id: String,
    val type: KType,
) {

    public companion object {

        public inline fun <reified T> create(id: String): StatementParameter<T> {
            return StatementParameter(id, typeOf<T>())
        }

        public inline fun <reified T> create(property: KProperty<T>): StatementParameter<T> {
            return StatementParameter(property.name, property.returnType)
        }

        public inline fun <reified T> create(parameter: KParameter): StatementParameter<T> {
            val id = parameter.name ?: throw IllegalArgumentException("Parameter '$parameter' has no name.")
            return StatementParameter(id, parameter.type)
        }
    }
}
