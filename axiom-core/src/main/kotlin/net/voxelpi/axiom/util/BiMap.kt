package net.voxelpi.axiom.util

public interface BiMap<K, V> : Map<K, V> {

    override val values: Set<V>

    public val inverse: BiMap<V, K>
}
