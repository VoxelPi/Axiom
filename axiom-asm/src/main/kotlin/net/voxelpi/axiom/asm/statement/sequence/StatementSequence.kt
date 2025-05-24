package net.voxelpi.axiom.asm.statement.sequence

import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import java.util.UUID

public interface StatementSequence {

    public val globalScope: GlobalScope

    public val statements: List<StatementInstance<*>>

    public val scopes: Map<UUID, Scope>

    public val anchors: Map<UUID, ScopeAnchor>
}
