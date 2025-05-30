package net.voxelpi.axiom.arch

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.register.RegisterFile

/**
 * @property id The id of the architecture.
 * @property instructionWordType The type of instruction word.
 * @property memorySize The size of the memory.
 * @property stackSize The size of the stack.
 */
public abstract class Architecture<P : Comparable<P>, I : Comparable<I>>(
    public val id: String,
    public val instructionWordType: WordType<I>,
    public val dataWordType: WordType<*>,
    public val memorySize: Int,
    public val stackSize: Int,
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
     * If the architecture has an encoded instruction format.
     */
    public open val hasEncodedFormat: Boolean
        get() = true

    /**
     * All operations that are supported by this architecture.
     */
    public abstract val supportedOperations: Set<Operation>

    /**
     * Encodes the given [instruction] into the architecture-specific format.
     */
    public abstract fun encodeInstruction(instruction: Instruction): Result<I>

    /**
     * Encodes the given [instruction] into the architecture-specific format.
     */
    public fun encodeInstructionPacked(instruction: Instruction): Result<UByteArray> {
        // Check if the architecture supports encoded programs.
        if (!hasEncodedFormat) {
            throw UnsupportedOperationException("The architecture \"${id}\" does not support encoded programs.")
        }

        return encodeInstruction(instruction).map { instructionWordType.pack(it) }
    }

    /**
     * Decodes the given architecture-specific [encodedInstruction] into an instruction.
     */
    public abstract fun decodeInstruction(encodedInstruction: I): Result<Instruction>

    /**
     * Decodes the given architecture-specific [encodedInstruction] into an instruction.
     */
    public fun decodeInstructionPacked(encodedInstruction: UByteArray): Result<Instruction> {
        // Check if the architecture supports encoded programs.
        if (!hasEncodedFormat) {
            throw UnsupportedOperationException("The architecture \"${id}\" does not support encoded programs.")
        }

        return decodeInstruction(instructionWordType.unpack(encodedInstruction))
    }

    /**
     * Encodes the program to an [UByteArray].
     */
    public fun encodeProgram(program: Program): Result<UByteArray> = runCatching {
        // Check if the architecture supports encoded programs.
        if (!hasEncodedFormat) {
            throw UnsupportedOperationException("The architecture \"${id}\" does not support encoded programs.")
        }

        // Calculate encoded program length.
        val encodedProgram = UByteArray(program.instructions.size * instructionWordType.bytes)

        // Encode instructions.
        for ((iInstruction, instruction) in program.instructions.withIndex()) {
            if (instruction.operation !in supportedOperations) {
                throw IllegalArgumentException("The operation \"${instruction.operation}\" is not supported by the architecture \"${id}\".")
            }
            val encodedInstruction = encodeInstructionPacked(instruction).getOrThrow()
            encodedInstruction.copyInto(encodedProgram, iInstruction * instructionWordType.bytes)
        }

        // Return encoded program.
        encodedProgram
    }

    /**
     * Decodes a program from the given [encodedProgram].
     */
    public fun decodedProgram(encodedProgram: UByteArray): Result<Program> = runCatching {
        // Check if the architecture supports encoded programs.
        if (!hasEncodedFormat) {
            throw UnsupportedOperationException("The architecture \"${id}\" does not support encoded programs.")
        }

        // Check encoded program length.
        require(encodedProgram.size % instructionWordType.bytes == 0) { "The encoded program must be a multiple of the instruction word size." }
        val nInstructions = encodedProgram.size / instructionWordType.bytes

        // Decode instructions.
        val instructions = mutableListOf<Instruction>()
        for (iInstruction in 0 until nInstructions) {
            val instruction = decodeInstructionPacked(
                encodedProgram.copyOfRange(
                    iInstruction * instructionWordType.bytes,
                    (iInstruction + 1) * instructionWordType.bytes,
                )
            ).getOrThrow()
            instructions += instruction
        }

        // Return decoded program.
        Program(instructions)
    }

    override fun toString(): String {
        return id
    }
}
