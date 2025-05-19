package net.voxelpi.axiom.arch

import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Register

public interface Architecture<W> {

    /**
     * The id of the architecture.
     */
    public val id: String

    /**
     * Encodes the given [instruction] into the architecture-specific format.
     */
    public fun encodeInstruction(instruction: Instruction): Result<UByteArray>

    /**
     * Decodes the given architecture-specific [encodedInstruction] into an instruction.
     */
    public fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction>

    /**
     * Returns the number of bits in a word of the architecture.
     */
    public fun wordSize(): Int

    /**
     * Returns all registers of the architecture.
     */
    public fun registers(): Collection<Register>

    /**
     * Returns the register with the given [id] or null if no such register exists.
     */
    public fun register(id: String): Register?
}
