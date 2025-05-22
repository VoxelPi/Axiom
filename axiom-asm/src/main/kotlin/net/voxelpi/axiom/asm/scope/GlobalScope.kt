package net.voxelpi.axiom.asm.scope

import java.util.UUID

public class GlobalScope : Scope {

    // The global scope has a unique id of 0.
    override val uniqueId: UUID = UUID(0, 0)
}
