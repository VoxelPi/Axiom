package net.voxelpi.axiom.arch

import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.register.RegisterFile

/**
 * @property id The id of the architecture.
 * @property memorySize The size of the memory.
 * @property stackSize The size of the stack.
 */
public abstract class Architecture<P : Comparable<P>>(
    public val id: String,
    public val memorySize: UInt,
    public val stackSize: UInt,
) {

    /**
     * The register file of the architecture.
     */
    public abstract val registers: RegisterFile<P>

    /**
     * The maximum size of a program on this architecture.
     */
    public val programSize: ULong
        get() = 1UL shl registers.programCounter.type.bits

    /**
     * Encodes the given [instruction] into the architecture-specific format.
     */
    public abstract fun encodeInstruction(instruction: Instruction): Result<UByteArray>

    /**
     * Decodes the given architecture-specific [encodedInstruction] into an instruction.
     */
    public abstract fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction>
}
