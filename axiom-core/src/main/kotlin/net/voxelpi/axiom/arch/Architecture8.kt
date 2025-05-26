package net.voxelpi.axiom.arch

import net.voxelpi.axiom.Register

public abstract class Architecture8(
    override val id: String,
) : Architecture<UByte> {

    override val programCounter: Register = createProgramCounter()

    private val registers: Map<String, Register> = createRegisters().associateBy { it.id }

    protected abstract fun createProgramCounter(): Register

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
