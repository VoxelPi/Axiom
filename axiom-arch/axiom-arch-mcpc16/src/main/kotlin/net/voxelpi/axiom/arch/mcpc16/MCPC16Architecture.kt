package net.voxelpi.axiom.arch.mcpc16

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.register.RegisterFile

public object MCPC16Architecture : Architecture<UShort, ULong>("mcpc16", WordType.INT64, 256U, 16U) {

    override val registers: RegisterFile<UShort> = RegisterFile.create("program_counter", WordType.INT16) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = false)

        for (registerIndex in 1..15) {
            val isConditional = registerIndex >= 14
            val conditionSourceIndex = registerIndex - 14 + 1

            val register = createRegister("R$registerIndex", WordType.INT8)
            createVariable("R$registerIndex", register, registerIndex, readable = true, writeable = true, conditionable = isConditional)
            if (isConditional) {
                createVariable("C$conditionSourceIndex", register, registerIndex, readable = true, writeable = true, conditionable = false)
            }
        }
    }

    override fun encodeInstruction(instruction: Instruction): Result<ULong> {
        TODO("Not yet implemented")
    }

    override fun decodeInstruction(encodedInstruction: ULong): Result<Instruction> {
        TODO("Not yet implemented")
    }
}
