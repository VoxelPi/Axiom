package net.voxelpi.axiom.arch.dev32

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.arch.MemoryMap
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile

public object DEV32Architecture : Architecture(
    "dev32",
    WordType.INT32,
    WordType.INT32,
    0x1_00_00,
    WordType.INT32,
) {

    override val registers: RegisterFile = RegisterFile.create("PC", WordType.INT32) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = true)
        for (iRegister in 1..255) {
            val register = createRegister("R$iRegister", WordType.INT32)
            createVariable("R$iRegister", register, iRegister, readable = true, writeable = true, conditionable = true)
        }
    }

    override val memoryMap: MemoryMap = MemoryMap.create(WordType.INT32) {
        memory(0x0000..0x7FFF)
        program(0x8000..0xFFFF, 0x8000..0xFFFF)
    }

    override val hasEncodedFormat: Boolean
        get() = false

    override val supportedOperations: Set<Operation>
        get() = Operation.entries.toSet()

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV32 architecture."))
    }

    override fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV32 architecture."))
    }
}
