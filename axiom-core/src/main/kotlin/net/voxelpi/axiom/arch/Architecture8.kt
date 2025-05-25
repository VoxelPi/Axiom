package net.voxelpi.axiom.arch

import net.voxelpi.axiom.Register

public abstract class Architecture8(
    override val id: String,
) : Architecture<UByte> {

    private val registers: MutableMap<String, Register> = mutableMapOf()

    protected abstract fun createRegisters(): Collection<Register>

    override fun registers(): Collection<Register> {
        return registers.values
    }

    override fun register(id: String): Register? {
        return registers[id]
    }

    override fun wordSize(): Int {
        return 8
    }
}
