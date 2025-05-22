package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.source.SourceLink
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class StatementArgument<T>(
    public val source: SourceLink,
    public val type: KType,
    public val value: T,
) {

    public companion object {
        public inline fun <reified T> create(source: SourceLink, value: T): StatementArgument<T> {
            return StatementArgument(source, typeOf<T>(), value)
        }

        public inline fun <reified T> generated(sourceText: String, generator: String, value: T): StatementArgument<T> {
            return StatementArgument(SourceLink.Generated(sourceText, generator), typeOf<T>(), value)
        }
    }
}
