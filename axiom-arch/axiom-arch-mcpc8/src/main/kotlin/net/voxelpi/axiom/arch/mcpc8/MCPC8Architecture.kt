package net.voxelpi.axiom.arch.mcpc8

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.register.RegisterFile

public object MCPC8Architecture : Architecture<UByte, UShort>("mcpc8", WordType.INT16, 256U, 256U) {

    override val registers: RegisterFile<UByte> = RegisterFile.create("program_counter", WordType.INT8) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = false)

        for (registerIndex in 1..7) {
            val isConditional = registerIndex == 7
            val conditionSourceIndex = 1

            val register = createRegister("register_$registerIndex", WordType.INT8)
            createVariable("R$registerIndex", register, registerIndex, readable = true, writeable = true, conditionable = isConditional)
            if (isConditional) {
                createVariable("C$conditionSourceIndex", register, registerIndex, readable = true, writeable = true, conditionable = true)
            }
        }
    }

    override fun encodeInstruction(instruction: Instruction): Result<UShort> {
        TODO("Not yet implemented")
    }

    override fun decodeInstruction(encodedInstruction: UShort): Result<Instruction> {
        TODO("Not yet implemented")
    }
}
