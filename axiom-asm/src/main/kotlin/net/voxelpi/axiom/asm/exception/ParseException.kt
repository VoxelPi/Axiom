package net.voxelpi.axiom.asm.exception

import net.voxelpi.axiom.asm.source.SourceLink

public class ParseException(
    source: SourceLink,
    message: String,
    cause: Throwable? = null,
) : SourceCompilationException(source, message, cause)
