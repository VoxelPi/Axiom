package net.voxelpi.axiom.instruction

public sealed interface ProgramElement {
    public val meta: Map<String, Any?>

    public data object None : ProgramElement {

        override val meta: Map<String, Any?> = emptyMap()
    }
}
