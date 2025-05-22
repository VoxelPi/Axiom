package net.voxelpi.axiom.asm

public data class CompilationUnit(
    public val id: String,
    public val content: String,
    public val parents: Set<String> = emptySet(),
)
