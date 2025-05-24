package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public class GlobalScope : Scope {

    // The global scope has a unique id of 0.
    override val uniqueId: UUID = UUID(0, 0)

    override val variables: Map<String, Variable> = emptyMap()

    override val labels: Map<String, Label> = emptyMap()
}
