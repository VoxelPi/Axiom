package net.voxelpi.axiom.asm.exception

import net.voxelpi.axiom.asm.source.SourceLink

public class ParseException(
    source: SourceLink,
    public val reason: String,
) : SourceCompilationException(source, "Unable to parse '${source.text}' ${if (source is SourceLink.CompilationUnitSlice) "at ${source.line}, ${source.column} of unit ${source.unit.id}" else ""}: $reason")
