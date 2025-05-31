package net.voxelpi.axiom.arch.dev16

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile

public object DEV16Architecture : Architecture<UShort, UShort>(
    "dev16",
    WordType.INT16,
    WordType.INT16,
    0x1_00_00,
    WordType.INT16,
    0x1_00_00,
    WordType.INT16,
) {

    override val registers: RegisterFile<UShort> = RegisterFile.create("PC", WordType.INT16) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = true)
        for (iRegister in 1..255) {
            val register = createRegister("R$iRegister", WordType.INT16)
            createVariable("R$iRegister", register, iRegister, readable = true, writeable = true, conditionable = true)
        }
    }

    override val hasEncodedFormat: Boolean
        get() = false

    override val supportedOperations: Set<Operation>
        get() = Operation.entries.toSet()

    override fun encodeInstruction(instruction: Instruction): Result<UShort> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV16 architecture."))
    }

    override fun decodeInstruction(encodedInstruction: UShort): Result<Instruction> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV16 architecture."))
    }
}
