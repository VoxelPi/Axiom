package net.voxelpi.axiom.asm.type

import net.voxelpi.axiom.asm.CompilationUnit

public sealed interface UnitLike {

    public data class UnitName(
        val name: String,
    ) : UnitLike

    public data class UnitReference(
        val unit: CompilationUnit,
    ) : UnitLike
}
