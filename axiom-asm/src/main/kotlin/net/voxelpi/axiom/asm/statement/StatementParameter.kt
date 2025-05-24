package net.voxelpi.axiom.asm.statement

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
    }
}
