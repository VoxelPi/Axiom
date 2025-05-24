package net.voxelpi.axiom.asm.statement

public sealed interface StatementSetElement {

    public val id: String

    public val sets: Set<StatementSet>

    public val parameters: Map<String, StatementParameter<*>>
}
