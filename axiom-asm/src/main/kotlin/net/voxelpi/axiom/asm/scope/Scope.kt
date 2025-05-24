package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public sealed interface Scope {

    public val uniqueId: UUID

    public val variables: Map<String, Variable>

    public val labels: Map<String, Label>
}
