package net.voxelpi.axiom.asm.statement.preprocessor

import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.Statement

public sealed interface IncludeStatement : Statement {

    public data class Unit(
        override val source: SourceLink,
        val unitId: String,
    ) : IncludeStatement

    public data class Scope(
        override val source: SourceLink,
        val unitId: String,
        val scopeId: String,
    ) : IncludeStatement

    public data class ScopeWithAlias(
        override val source: SourceLink,
        val unitId: String,
        val scopeId: String,
        val alias: String,
    ) : IncludeStatement
}
