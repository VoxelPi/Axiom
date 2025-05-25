package net.voxelpi.axiom.arch.mcpc16

import net.voxelpi.axiom.Register
import net.voxelpi.axiom.arch.Architecture16
import net.voxelpi.axiom.instruction.Instruction

public object MCPC16Architecture : Architecture16("mcpc16") {

    override val programCounter: Register = Register("PC")

    override fun createRegisters(): Collection<Register> {
        val registers = mutableListOf<Register>()
        registers.add(programCounter)
        for (i in 1..15) {
            registers.add(Register("R$i"))
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
