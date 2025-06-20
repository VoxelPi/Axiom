package net.voxelpi.axiom.arch

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.instruction.ProgramConstant
import net.voxelpi.axiom.instruction.ProgramElement
import net.voxelpi.axiom.register.RegisterFile

/**
 * @property id The id of the architecture.
 * @property instructionWordType The type of instruction word.
 * @property dataWordType The type of data word.
 * @property stackSize The size of the stack.
 * @property stackWordType The type of stack data word.
 */
public abstract class Architecture(
    public val id: String,
    public val instructionWordType: WordType,
    public val dataWordType: WordType,
    public val stackSize: Int,
    public val stackWordType: WordType,
) {

    /**
     * The register file of the architecture.
     */
    public abstract val registers: RegisterFile

    /**
     * The memory map of the architecture.
     */
    public abstract val memoryMap: MemoryMap

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
    public abstract fun encodeInstruction(instruction: Instruction): Result<UByteArray>

    /**
     * Decodes the given architecture-specific [encodedInstruction] into an instruction.
     */
    public abstract fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction>

    /**
     * Encodes the program to an [UByteArray].
     */
    public fun encodeProgram(program: Program, invertByteOrder: Boolean = false): Result<UByteArray> = runCatching {
        // Check if the architecture supports encoded programs.
        if (!hasEncodedFormat) {
            throw UnsupportedOperationException("The architecture \"${id}\" does not support encoded programs.")
        }

        // Calculate encoded program length.
        val encodedProgram = UByteArray(program.data.size * instructionWordType.bytes)

        // Encode instructions.
        for ((iInstruction, element) in program.data.withIndex()) {
            val data = when (element) {
                is Instruction -> {
                    val instruction = element
                    if (instruction.operation !in supportedOperations) {
                        throw IllegalArgumentException("The operation \"${instruction.operation}\" is not supported by the architecture \"${id}\".")
                    }
                    encodeInstruction(instruction).getOrThrow()
                }
                is ProgramConstant -> {
                    instructionWordType.pack(element.value)
                }
                is ProgramElement.None -> {
                    instructionWordType.pack(0UL)
                }
            }
            if (invertByteOrder) {
                data.reverse()
            }
            data.copyInto(encodedProgram, iInstruction * instructionWordType.bytes)
        }

        // Return encoded program.
        encodedProgram
    }

    /**
     * Decodes a program from the given [encodedProgram].
     */
    public fun decodedProgram(encodedProgram: UByteArray, invertByteOrder: Boolean = false): Result<Program> = runCatching {
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
            val instructionData = encodedProgram.copyOfRange(
                iInstruction * instructionWordType.bytes,
                (iInstruction + 1) * instructionWordType.bytes,
            )
            if (invertByteOrder) {
                instructionData.reverse()
            }
            val instruction = decodeInstruction(instructionData).getOrThrow()
            instructions += instruction
        }

        // Return decoded program.
        Program(instructions)
    }

    override fun toString(): String {
        return id
    }
}
