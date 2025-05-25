package net.voxelpi.axiom.asm.exception

import net.voxelpi.axiom.asm.source.SourceLink

public class ParseException(
    source: SourceLink,
    public val reason: String,
) : SourceCompilationException(source, "Unable to parse '${source.text}' ${if (source is SourceLink.CompilationUnitSlice) "at line ${source.line + 1}, ${source.column + 1} of unit ${source.unit.id}" else ""}: $reason")
