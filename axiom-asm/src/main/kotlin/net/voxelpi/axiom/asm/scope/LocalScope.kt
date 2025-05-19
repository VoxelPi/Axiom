package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.statement.Statement

public sealed class LocalScope(
    public val parent: Scope,
    public val statements: List<Statement>,
) : Scope {

    public class Named(
        parent: Scope,
        statements: List<Statement>,
        public val name: String,
    ) : LocalScope(parent, statements)

    public class Unnamed(
        parent: Scope,
        statements: List<Statement>,
    ) : LocalScope(parent, statements)
}
