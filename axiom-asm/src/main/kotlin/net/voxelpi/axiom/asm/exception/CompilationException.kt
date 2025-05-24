package net.voxelpi.axiom.asm.exception

public open class CompilationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
