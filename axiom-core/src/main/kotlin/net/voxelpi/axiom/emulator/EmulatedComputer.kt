package net.voxelpi.axiom.emulator

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Program

public class EmulatedComputer<P : Comparable<P>>(
    public val architecture: Architecture<P, *>,
    public val program: Program,
)
