package net.voxelpi.axiom.asm

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.compositor.Compositor
import net.voxelpi.axiom.asm.frontend.lexer.Lexer
import net.voxelpi.axiom.asm.source.SourceUnit
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
        mainUnitText: String,
        architecture: Architecture,
        unitTextProvider: (id: String) -> String?,
    ) {
        val mainUnit = SourceUnit("__main__", mainUnitText)
        val unitProvider = { id: String ->
            unitTextProvider(id)?.let { text -> SourceUnit(id, text) }
        }

        val mainUnitTokens = Lexer.tokenize(mainUnit)
        val unitTokenProvider = { id: String ->
            unitProvider(id)?.let { unit -> Lexer.tokenize(unit) }
        }

        Compositor.composite(mainUnitTokens, unitTokenProvider)
    }
}
