package net.voxelpi.axiom.asm.language

public data class NamespacedId(
    val parts: List<String>,
) {

    public fun createChild(name: String): NamespacedId {
        return NamespacedId(parts + listOf(name))
    }

    override fun toString(): String {
        return parts.joinToString("::")
    }

    public companion object {
        public val GLOBAL: NamespacedId = NamespacedId(emptyList())
    }
}
