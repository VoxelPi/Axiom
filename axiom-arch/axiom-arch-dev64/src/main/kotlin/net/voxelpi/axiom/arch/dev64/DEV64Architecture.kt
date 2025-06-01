package net.voxelpi.axiom.arch.dev64

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile

public object DEV64Architecture : Architecture(
    "dev64",
    WordType.INT64,
    WordType.INT64,
    1_000_000,
    WordType.INT64,
    0x1_00_00,
    WordType.INT64,
) {

    override val registers: RegisterFile = RegisterFile.create("PC", WordType.INT64) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = true)
        for (iRegister in 1..255) {
            val register = createRegister("R$iRegister", WordType.INT64)
            createVariable("R$iRegister", register, iRegister, readable = true, writeable = true, conditionable = true)
        }
    }

    override val hasEncodedFormat: Boolean
        get() = false

    override val supportedOperations: Set<Operation>
        get() = Operation.entries.toSet()

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV64 architecture."))
    }

    override fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV64 architecture."))
    }
}
