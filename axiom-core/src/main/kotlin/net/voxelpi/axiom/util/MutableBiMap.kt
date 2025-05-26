package net.voxelpi.axiom.util

public class MutableBiMap<K, V>(
    private val direct: MutableMap<K, V>,
    private val reverse: MutableMap<V, K>,
) : BiMap<K, V>, MutableMap<K, V> {

    override val values: MutableSet<V>
        get() = reverse.keys

    override val inverse: BiMap<V, K>
        get() = MutableBiMap(reverse, direct)

    override val size: Int
        get() = direct.size

    override fun isEmpty(): Boolean {
        return direct.isEmpty()
    }

    override fun containsKey(key: K): Boolean {
        return direct.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return reverse.containsKey(value)
    }

    override fun get(key: K): V? {
        return direct[key]
    }

    override val keys: MutableSet<K>
        get() = direct.keys

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = direct.entries

    override fun put(key: K, value: V): V? {
        reverse.put(value, key)
        return direct.put(key, value)
    }

    override fun remove(key: K): V? {
        val value = direct.remove(key)
        reverse.remove(value)
        return value
    }

    override fun putAll(from: Map<out K, V>) {
        direct.putAll(from)
        reverse.putAll(from.entries.associate { it.value to it.key })
    }

    override fun clear() {
        direct.clear()
        reverse.clear()
    }
}
