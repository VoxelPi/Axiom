package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.source.SourceLink

public class GlobalScope : Scope {

    override val source: SourceLink
        get() = SourceLink.Generated("__global__", "__global__")
}
