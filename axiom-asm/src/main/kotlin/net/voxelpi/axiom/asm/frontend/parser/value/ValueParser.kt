package net.voxelpi.axiom.asm.frontend.parser.value

import net.voxelpi.axiom.asm.frontend.parser.TokenReader
import kotlin.reflect.KType

internal abstract class ValueParser<T : Any>(
    val type: KType,
) {

    abstract fun parse(tokens: TokenReader): Result<T>
}
