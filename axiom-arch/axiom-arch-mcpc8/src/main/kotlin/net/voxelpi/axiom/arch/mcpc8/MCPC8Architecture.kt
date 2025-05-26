package net.voxelpi.axiom.arch.mcpc8

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.register.RegisterFile

public object MCPC8Architecture : Architecture<UByte>("mcpc8", 256U, 256U) {

    override val registers: RegisterFile<UByte> = RegisterFile.create("program_counter", WordType.INT8) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = false)

        for (registerIndex in 1..7) {
            val isConditional = registerIndex == 7
            val conditionSourceIndex = 1

            val register = createRegister("R$registerIndex", WordType.INT8)
            createVariable("R$registerIndex", register, registerIndex, readable = true, writeable = true, conditionable = isConditional)
            if (isConditional) {
                createVariable("C$conditionSourceIndex", register, registerIndex, readable = true, writeable = true, conditionable = false)
            }
        }
    }

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        TODO("Not yet implemented")
    }

    override fun decodeInstruction(encodedInstructionBytes: UByteArray): Result<Instruction> {
        TODO("Not yet implemented")
    }
}
