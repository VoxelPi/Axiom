package net.voxelpi.axiom.arch.ax08

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.register.RegisterFile

public object AX08Architecture : Architecture<UShort>("ax08", 256U, 16U) {

    override val registers: RegisterFile<UShort> = RegisterFile.create("program_counter", WordType.INT16) {
        // Create the PC variables.
        programCounterVariable = createVariable("PC", programCounter, 14, readable = false, writeable = true, conditionable = false)
        createVariable("PC_0", programCounter, WordType.INT8, 0, 14, readable = true, writeable = false, conditionable = false)
        createVariable("PC_1", programCounter, WordType.INT8, 1, 15, readable = true, writeable = false, conditionable = false)

        // Create general purpose registers.
        repeat(8) { registerIndex ->
            val register = createRegister("R${registerIndex}", WordType.INT8)
            createVariable("R${registerIndex}", register, registerIndex, readable = true, writeable = true, conditionable = true)
        }
    }

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        TODO("Not yet implemented")
    }

    override fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction> {
        TODO("Not yet implemented")
    }
}
