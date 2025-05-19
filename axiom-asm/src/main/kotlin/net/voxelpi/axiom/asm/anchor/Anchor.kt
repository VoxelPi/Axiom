package net.voxelpi.axiom.asm.anchor

import net.voxelpi.axiom.asm.statement.Statement

public sealed interface Anchor : Statement {

    public interface Named : Anchor {

        public val name: String
    }

    public class Unnamed : Anchor
}
