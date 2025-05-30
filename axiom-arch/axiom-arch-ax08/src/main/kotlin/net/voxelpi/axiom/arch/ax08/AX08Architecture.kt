package net.voxelpi.axiom.arch.ax08

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile
import net.voxelpi.axiom.register.RegisterVariable
import net.voxelpi.axiom.util.biMapOf

public object AX08Architecture : Architecture<UShort, UInt>(
    "ax08",
    WordType.INT32,
    WordType.INT8,
    256,
    WordType.INT8,
    16,
    WordType.INT16,
) {

    override val registers: RegisterFile<UShort> = RegisterFile.create("PC", WordType.INT16) {
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

    override val supportedOperations: Set<Operation>
        get() = operationMapping.values

    override fun encodeInstruction(instruction: Instruction): Result<UInt> {
        var encodedInstruction = 0.toUInt()

        // Encode operation.
        encodedInstruction = encodedInstruction or ((operationMapping.inverse[instruction.operation]!! and 0b11111u) shl 27)

        // Encode condition.
        encodedInstruction = encodedInstruction or ((conditionMapping.inverse[instruction.condition]!! and 0b111u) shl 24)

        // Encode condition register.
        encodedInstruction = encodedInstruction or ((instruction.conditionRegister.address.toUInt() and 0b111u) shl 21)

        // Encode output register address (except if the instruction has an explicit output to the program counter).
        if (instruction.operation !in pcOutputOperations) {
            encodedInstruction = encodedInstruction or ((instruction.outputRegister.address.toUInt() and 0b111u) shl 18)
        }

        // Encode input A.
        encodedInstruction = when (val value = instruction.inputA) {
            is InstructionValue.ImmediateValue -> {
                encodedInstruction or ((value.value.toUInt() and 0xFFu) shl 0)
            }
            is InstructionValue.RegisterReference -> {
                encodedInstruction or ((value.register.address.toUInt() and 0xFFu) shl 0) or (1u shl 16)
            }
        }

        // Encode input B.
        encodedInstruction = when (val value = instruction.inputB) {
            is InstructionValue.ImmediateValue -> {
                encodedInstruction or ((value.value.toUInt() and 0xFFu) shl 8)
            }
            is InstructionValue.RegisterReference -> {
                encodedInstruction or ((value.register.address.toUInt() and 0xFFu) shl 8) or (1u shl 17)
            }
        }

        // Return encoded instruction.
        return Result.success(encodedInstruction)
    }

    override fun decodeInstruction(encodedInstruction: UInt): Result<Instruction> {
        // Decode the operation.
        val operation = operationMapping[((encodedInstruction shr 27) and 0b11111u)]
            ?: return Result.failure(IllegalArgumentException("Invalid opcode ${((encodedInstruction shr 27) and 0b11111u)}"))

        // Decode the condition.
        val condition = conditionMapping[((encodedInstruction shr 24) and 0b111u)]
            ?: return Result.failure(IllegalArgumentException("Invalid condition ${((encodedInstruction shr 24) and 0b111u)}"))

        // Decode the condition register.
        val conditionRegisterAddress = ((encodedInstruction shr 21) and 0b111u).toInt()
        val conditionRegister = registers.variables.values.find {
            it.address == conditionRegisterAddress && it.conditionable
        } ?: return Result.failure(IllegalArgumentException("Invalid condition register address $conditionRegisterAddress"))

        // Decode the output register.
        val outputRegister: RegisterVariable<*, *> = if (operation in pcOutputOperations) {
            registers.programCounterVariable
        } else {
            val outputRegisterAddress = ((encodedInstruction shr 18) and 0b111u).toInt()
            registers.variables.values.find { it.address == outputRegisterAddress && it.writable }
                ?: return Result.failure(IllegalArgumentException("Invalid output register address $outputRegisterAddress"))
        }

        // Decode input A.
        val inputA = if ((encodedInstruction shr 16) and 1u != 0u) {
            val registerAddress = ((encodedInstruction shr 0) and 0xFu).toInt()
            val register = registers.variables.values.find { it.address == registerAddress && it.readable }
                ?: return Result.failure(IllegalArgumentException("Invalid input A register address $registerAddress"))
            InstructionValue.RegisterReference(register)
        } else {
            InstructionValue.ImmediateValue(((encodedInstruction shr 0) and 0xFFu).toLong())
        }

        // Decode input B.
        val inputB = if ((encodedInstruction shr 17) and 1u != 0u) {
            val registerAddress = ((encodedInstruction shr 8) and 0xFu).toInt()
            val register = registers.variables.values.find { it.address == registerAddress && it.readable }
                ?: return Result.failure(IllegalArgumentException("Invalid input B register address $registerAddress"))
            InstructionValue.RegisterReference(register)
        } else {
            InstructionValue.ImmediateValue(((encodedInstruction shr 8) and 0xFFu).toLong())
        }

        // Return decoded instruction.
        return Result.success(Instruction(operation, condition, conditionRegister, outputRegister, inputA, inputB))
    }

    private val conditionMapping = biMapOf(
        0u to Condition.ALWAYS,
        1u to Condition.NEVER,
        2u to Condition.EQUAL,
        3u to Condition.NOT_EQUAL,
        4u to Condition.LESS,
        5u to Condition.GREATER_OR_EQUAL,
        6u to Condition.GREATER,
        7u to Condition.LESS_OR_EQUAL,
    )

    private val operationMapping = biMapOf(
        0x00u to Operation.LOAD_2,
        0x01u to Operation.LOAD,
        0x02u to Operation.AND,
        0x03u to Operation.NAND,
        0x04u to Operation.OR,
        0x05u to Operation.NOR,
        0x06u to Operation.XOR,
        0x07u to Operation.XNOR,
        0x08u to Operation.ADD,
        0x09u to Operation.SUBTRACT,
        0x0Au to Operation.ADD_WITH_CARRY,
        0x0Bu to Operation.SUBTRACT_WITH_CARRY,
        0x0Cu to Operation.SHIFT_LEFT,
        0x0Du to Operation.SHIFT_RIGHT,
        0x0Eu to Operation.ROTATE_LEFT,
        0x0Fu to Operation.ROTATE_RIGHT,
        0x10u to Operation.BIT_GET,
        0x11u to Operation.BIT_SET,
        0x12u to Operation.BIT_CLEAR,
        0x13u to Operation.BIT_TOGGLE,
        0x14u to Operation.MEMORY_LOAD,
        0x15u to Operation.MEMORY_STORE,
        // 0x16 is not implemented
        // 0x17 is not implemented
        0x18u to Operation.IO_POLL,
        0x19u to Operation.IO_READ,
        0x1Au to Operation.IO_WRITE,
        // 0x1B is not implemented
        0x1Cu to Operation.CALL_2,
        0x1Du to Operation.RETURN,
        // 0x1E is not implemented
        0x1Fu to Operation.BREAK,
    )

    private val pcOutputOperations = setOf(Operation.LOAD_2, Operation.CALL_2)
}
