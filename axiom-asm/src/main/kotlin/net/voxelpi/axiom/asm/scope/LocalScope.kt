package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.Statement

public sealed class LocalScope(
    public override val source: SourceLink,
    public val parent: Scope,
    public val statements: List<Statement>,
) : Scope {

    public class Named(
        source: SourceLink,
        parent: Scope,
        statements: List<Statement>,
        public val name: String,
    ) : LocalScope(source, parent, statements)

    public class Unnamed(
        source: SourceLink,
        parent: Scope,
        statements: List<Statement>,
    ) : LocalScope(source, parent, statements)
}
