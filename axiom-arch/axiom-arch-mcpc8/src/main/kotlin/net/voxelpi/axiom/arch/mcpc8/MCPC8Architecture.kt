package net.voxelpi.axiom.arch.mcpc8

import net.voxelpi.axiom.arch.Architecture8
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Register

public object MCPC8Architecture : Architecture8("mcpc8") {

    override fun createRegisters(): Collection<Register> {
        val registers = mutableListOf<Register>()
        registers.add(Register("pc"))
        for (i in 1..7) {
            registers.add(Register("r$i"))
        }
        return registers
    }

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        TODO("Not yet implemented")
    }

    override fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction> {
        TODO("Not yet implemented")
    }
}
