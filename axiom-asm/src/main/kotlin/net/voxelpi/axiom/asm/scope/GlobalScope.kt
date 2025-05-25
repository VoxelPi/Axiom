package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public class GlobalScope(
    override val scopes: MutableList<Scope> = mutableListOf(),
    override val variables: MutableMap<String, Variable> = mutableMapOf(),
    override val labels: MutableMap<String, Label> = mutableMapOf(),
) : Scope {

    // The global scope has a unique id of 0.
    override val uniqueId: UUID = UUID(0, 0)
}
