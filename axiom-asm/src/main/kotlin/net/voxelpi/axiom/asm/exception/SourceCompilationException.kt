package net.voxelpi.axiom.asm.exception

import net.voxelpi.axiom.asm.source.SourceLink

public open class SourceCompilationException(
    public val source: SourceLink,
    message: String,
    cause: Throwable? = null,
) : CompilationException(message, cause)
