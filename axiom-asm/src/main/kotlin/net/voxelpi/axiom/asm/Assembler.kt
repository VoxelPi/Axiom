package net.voxelpi.axiom.asm

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Program
import java.nio.file.Path

public object Assembler {

    public fun assemble(
        programPath: Path,
        architecture: Architecture,
        includeDirectories: List<Path>,
    ): Program {
        TODO()
    }

    public fun assemble(
        mainUnit: String,
        architecture: Architecture,
        includedUnits: Map<String, String> = emptyMap(),
    ) {
        TODO()
    }
}
