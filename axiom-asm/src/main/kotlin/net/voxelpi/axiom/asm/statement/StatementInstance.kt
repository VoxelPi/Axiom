package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink

public data class StatementInstance(
    public val statement: Statement,
    public val source: SourceLink,
    public val parameterValues: Map<String, *>,
    public val parameterSources: Map<String, SourceLink>,
    public val scope: Scope,
) {

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(parameter: StatementParameter<T>): T {
        return parameterValues[parameter.id] as T
    }
}
