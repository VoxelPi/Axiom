package net.voxelpi.axiom.asm.type

import java.util.UUID

public sealed interface VariableLike : RegisterLike {

    public data class VariableName(
        val name: String,
    ) : VariableLike

    public data class VariableReference(
        val id: UUID,
    ) : VariableLike
}
