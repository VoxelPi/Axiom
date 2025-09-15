package net.voxelpi.axiom.asm.exception

import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.Sourced

public class SourcedCompilationException(
    override val source: SourceReference,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause), Sourced
