package net.voxelpi.axiom.asm.parser.exception

import net.voxelpi.axiom.asm.source.SourceLink

public class ParseException(
    public val source: SourceLink,
    public val reason: String,
) : Exception("Unable to parse '${source.text}' ${if (source is SourceLink.CompilationUnitSlice) "at ${source.line}, ${source.column} of unit ${source.unit.id}" else ""}: $reason")
