package net.voxelpi.axiom.asm.anchor

import java.util.UUID

public sealed interface Anchor {

    public val uniqueId: UUID

    public interface Named : Anchor {

        public val name: String
    }

    public data class Unnamed(
        override val uniqueId: UUID,
    ) : Anchor
}
