package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.source.SourceLink
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public data class ParsedValue<T>(
    public val source: SourceLink,
    public val type: KType,
    public val value: T,
) {

    public companion object {
        public inline fun <reified T> create(source: SourceLink, value: T): ParsedValue<T> {
            return ParsedValue(source, typeOf<T>(), value)
        }
    }
}
