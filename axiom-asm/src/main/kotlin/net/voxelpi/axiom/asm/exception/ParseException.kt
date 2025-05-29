package net.voxelpi.axiom.asm.exception

import net.voxelpi.axiom.asm.source.SourceLink

public class ParseException(
    source: SourceLink,
    public val reason: String,
) : SourceCompilationException(source, "Failed to parse '${source.text}' ${if (source is SourceLink.CompilationUnitSlice) "at ${source.line + 1},${source.column + 1} of unit \"${source.unit.id}\"" else ""}: $reason")
