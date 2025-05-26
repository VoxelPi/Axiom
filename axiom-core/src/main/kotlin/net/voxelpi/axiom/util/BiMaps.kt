package net.voxelpi.axiom.util

public fun <K, V> biMapOf(vararg pairs: Pair<K, V>): BiMap<K, V> {
    return MutableBiMap(mutableMapOf(*pairs), mutableMapOf(*pairs.map { it.second to it.first }.toTypedArray()))
}

public fun <K, V> mutableBiMapOf(vararg pairs: Pair<K, V>): MutableBiMap<K, V> {
    return MutableBiMap(mutableMapOf(*pairs), mutableMapOf(*pairs.map { it.second to it.first }.toTypedArray()))
}
