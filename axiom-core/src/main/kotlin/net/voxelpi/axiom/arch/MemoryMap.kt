package net.voxelpi.axiom.arch

import net.voxelpi.axiom.WordType

public class MemoryMap internal constructor(
    public val wordType: WordType,
    public val mappings: List<MemoryMapping>,
) {
    public val memorySize: Int = mappings.filterIsInstance<MemoryMapping.Memory>().maxOfOrNull { it.addressRange.endInclusive + 1 } ?: 0

    public val size: Int = mappings.maxOfOrNull { it.addressRange.endInclusive + 1 } ?: 0

    public fun selectMapping(address: Int): MemoryMapping? {
        return mappings.find { address in it.addressRange }
    }

    public sealed interface MemoryMapping {

        public val addressRange: IntRange

        public data class Memory(
            override val addressRange: IntRange,
            val targetAddressRange: IntRange,
        ) : MemoryMapping {

            init {
                require((addressRange.endInclusive - addressRange.start) == (targetAddressRange.endInclusive - targetAddressRange.start)) {
                    "Mappings do not match."
                }
            }

            public fun map(address: Int): Int {
                require(address in addressRange) { "Address is out of range." }
                return targetAddressRange.start + (address - addressRange.start)
            }
        }

        public data class Program(
            override val addressRange: IntRange,
            val programAddressRange: IntRange,
        ) : MemoryMapping {
            init {
                require((addressRange.endInclusive - addressRange.start) == (programAddressRange.endInclusive - programAddressRange.start)) {
                    "Mappings do not match."
                }
            }

            public fun map(address: Int): Int {
                require(address in addressRange) { "Address is out of range." }
                return programAddressRange.start + (address - addressRange.start)
            }
        }
    }

    public companion object {

        public fun create(
            wordType: WordType,
            block: Builder.() -> Unit,
        ): MemoryMap {
            val builder = Builder(wordType)
            builder.block()
            return builder.build()
        }
    }

    public class Builder internal constructor(
        public val wordType: WordType,
    ) {
        public val mappings: MutableList<MemoryMapping> = mutableListOf()

        public fun memory(addressRange: IntRange, targetAddressRange: IntRange = addressRange): MemoryMapping.Memory {
            val mapping = MemoryMapping.Memory(addressRange, targetAddressRange)
            mappings += mapping
            return mapping
        }

        public fun program(addressRange: IntRange, programAddressRange: IntRange): MemoryMapping.Program {
            val mapping = MemoryMapping.Program(addressRange, programAddressRange)
            mappings += mapping
            return mapping
        }

        internal fun build(): MemoryMap {
            return MemoryMap(wordType, mappings.toList())
        }
    }
}
