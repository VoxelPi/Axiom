package net.voxelpi.axiom.arch.dev08

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.arch.MemoryMap
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile

public object DEV08Architecture : Architecture(
    "dev08",
    WordType.INT8,
    WordType.INT8,
    256,
    WordType.INT8,
) {

    override val registers: RegisterFile = RegisterFile.create("PC", WordType.INT8) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = true)
        for (iRegister in 1..255) {
            val register = createRegister("R$iRegister", WordType.INT8)
            createVariable("R$iRegister", register, iRegister, readable = true, writeable = true, conditionable = true)
        }
    }

    override val memoryMap: MemoryMap = MemoryMap.create(WordType.INT8) {
        memory(0x00..0xFF)
    }

    override val hasEncodedFormat: Boolean
        get() = false

    override val supportedOperations: Set<Operation>
        get() = Operation.entries.toSet()

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV08 architecture."))
    }

    override fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction> {
        return Result.failure(UnsupportedOperationException("Encoding of instructions is not supported by the DEV08 architecture."))
    }
}
