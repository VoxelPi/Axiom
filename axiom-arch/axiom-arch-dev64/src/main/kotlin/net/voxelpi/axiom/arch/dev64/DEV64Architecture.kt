package net.voxelpi.axiom.arch.dev64

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile

public object DEV64Architecture : Architecture<ULong, ULong>("dev64", WordType.INT64, WordType.INT64, 1_000_000, 0x1_00_00) {

    override val registers: RegisterFile<ULong> = RegisterFile.create("program_counter", WordType.INT64) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = true)
        repeat(255) { iRegister ->
            val register = createRegister("register_$iRegister", WordType.INT64)
            createVariable("R$iRegister", register, iRegister, readable = true, writeable = true, conditionable = true)
        }
    }

    override val hasEncodedFormat: Boolean
        get() = false

    override val supportedOperations: Set<Operation>
        get() = Operation.entries.toSet()

    override fun encodeInstruction(instruction: Instruction): Result<ULong> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV64 architecture."))
    }

    override fun decodeInstruction(encodedInstruction: ULong): Result<Instruction> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV64 architecture."))
    }
}
