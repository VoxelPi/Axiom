package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.statement.Statement
import java.util.UUID

public sealed interface Anchor : Statement {

    public val uniqueId: UUID

    public interface Named : Anchor {

        public val name: String
    }

    public interface Unnamed : Anchor
}
