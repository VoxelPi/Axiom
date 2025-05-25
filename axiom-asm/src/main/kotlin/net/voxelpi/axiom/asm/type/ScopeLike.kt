package net.voxelpi.axiom.asm.type

import net.voxelpi.axiom.asm.scope.Scope

public sealed interface ScopeLike : AnchorLike {

    /**
     * A reference to the scope with the given [name].
     */
    public data class ScopeName(
        public val name: String,
    ) : ScopeLike

    /**
     * The parent scope of a statement.
     */
    public data object ParentScope : ScopeLike

    public data class ScopeReference(
        val scope: Scope,
    ) : ScopeLike
}
