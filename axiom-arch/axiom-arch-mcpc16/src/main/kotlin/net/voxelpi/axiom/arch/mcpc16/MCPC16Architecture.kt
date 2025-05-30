package net.voxelpi.axiom.arch.mcpc16

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile
import net.voxelpi.axiom.util.biMapOf

public object MCPC16Architecture : Architecture<UShort, ULong>(
    "mcpc16",
    WordType.INT64,
    WordType.INT16,
    256,
    WordType.INT16,
    16,
    WordType.INT16,
) {

    override val registers: RegisterFile<UShort> = RegisterFile.create("PC", WordType.INT16) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = false)

        for (registerIndex in 1..15) {
            val isConditional = registerIndex >= 14
            val conditionSourceIndex = registerIndex - 14 + 1

            val register = createRegister("R$registerIndex", WordType.INT8)
            createVariable("R$registerIndex", register, registerIndex, readable = true, writeable = true, conditionable = isConditional)
            if (isConditional) {
                createVariable("C$conditionSourceIndex", register, registerIndex, readable = true, writeable = true, conditionable = true)
            }
        }
    }

    override val supportedOperations: Set<Operation>
        get() = operationMapping.values

    override fun encodeInstruction(instruction: Instruction): Result<ULong> {
        var encodedInstruction = 0.toULong()

        // Encode operation.
        encodedInstruction = encodedInstruction or ((operationMapping.inverse[instruction.operation]!! and 0b111111u) shl 10)

        // Encode condition.
        encodedInstruction = encodedInstruction or ((conditionMapping.inverse[instruction.condition]!! and 0b111u) shl 6)

        // Encode condition register.
        val conditionSource = conditionSourceMapping.inverse[instruction.conditionRegister.address]
            ?: return Result.failure(IllegalArgumentException("Invalid condition register address ${instruction.conditionRegister.id}"))
        encodedInstruction = encodedInstruction or ((conditionSource and 0b1u) shl 9)

        // Encode output register address.
        encodedInstruction = encodedInstruction or ((instruction.outputRegister.address.toULong() and 0b1111u) shl 2)

        // Encode input A.
        encodedInstruction = when (val value = instruction.inputA) {
            is InstructionValue.ImmediateValue -> {
                encodedInstruction or ((value.value.toULong() and 0xFFFFu) shl 16)
            }
            is InstructionValue.RegisterReference -> {
                encodedInstruction or ((value.register.address.toULong() and 0xFFFFu) shl 16) or (1u shl 0)
            }
        }

        // Encode input B.
        encodedInstruction = when (val value = instruction.inputB) {
            is InstructionValue.ImmediateValue -> {
                encodedInstruction or ((value.value.toULong() and 0xFFFFu) shl 32)
            }
            is InstructionValue.RegisterReference -> {
                encodedInstruction or ((value.register.address.toULong() and 0xFFFFu) shl 32) or (1u shl 1)
            }
        }

        // Return encoded instruction.
        return Result.success(encodedInstruction)
    }

    override fun decodeInstruction(encodedInstruction: ULong): Result<Instruction> {
        // Decode the operation.
        val operation = operationMapping[((encodedInstruction shr 10) and 0b111111u)]
            ?: return Result.failure(IllegalArgumentException("Invalid opcode ${((encodedInstruction shr 10) and 0b111111u)}"))

        // Decode the condition.
        val condition = conditionMapping[((encodedInstruction shr 5) and 0b111u)]
            ?: return Result.failure(IllegalArgumentException("Invalid condition ${((encodedInstruction shr 5) and 0b111u)}"))

        // Decode the condition register.
        val conditionSource = ((encodedInstruction shr 9) and 0b1u)
        val conditionRegisterAddress = conditionSourceMapping[conditionSource]
            ?: return Result.failure(IllegalArgumentException("Invalid condition source $conditionSource"))
        val conditionRegister = registers.variables.values.find {
            it.address == conditionRegisterAddress && it.conditionable
        } ?: return Result.failure(IllegalArgumentException("Invalid condition register address $conditionRegisterAddress"))

        // Decode the output register.
        val outputRegisterAddress = ((encodedInstruction shr 2) and 0b1111u).toInt()
        val outputRegister = registers.variables.values.find { it.address == outputRegisterAddress && it.writable }
            ?: return Result.failure(IllegalArgumentException("Invalid output register address $outputRegisterAddress"))

        // Decode input A.
        val inputA = if ((encodedInstruction shr 0) and 1uL != 0uL) {
            val registerAddress = ((encodedInstruction shr 16) and 0xFFFFu).toInt()
            val register = registers.variables.values.find { it.address == registerAddress && it.readable }
                ?: return Result.failure(IllegalArgumentException("Invalid input A register address $registerAddress"))
            InstructionValue.RegisterReference(register)
        } else {
            InstructionValue.ImmediateValue(((encodedInstruction shr 16) and 0xFFFFu).toLong())
        }

        // Decode input B.
        val inputB = if ((encodedInstruction shr 1) and 1uL != 0uL) {
            val registerAddress = ((encodedInstruction shr 32) and 0xFFFFu).toInt()
            val register = registers.variables.values.find { it.address == registerAddress && it.readable }
                ?: return Result.failure(IllegalArgumentException("Invalid input B register address $registerAddress"))
            InstructionValue.RegisterReference(register)
        } else {
            InstructionValue.ImmediateValue(((encodedInstruction shr 32) and 0xFFFFu).toLong())
        }

        // Return decoded instruction.
        return Result.success(Instruction(operation, condition, conditionRegister, outputRegister, inputA, inputB))
    }

    private val conditionSourceMapping = biMapOf(
        0uL to 14,
        1uL to 15,
    )

    private val conditionMapping = biMapOf(
        0uL to Condition.ALWAYS,
        1uL to Condition.NEVER,
        2uL to Condition.EQUAL,
        3uL to Condition.NOT_EQUAL,
        4uL to Condition.LESS,
        5uL to Condition.GREATER_OR_EQUAL,
        6uL to Condition.GREATER,
        7uL to Condition.LESS_OR_EQUAL,
    )

    private val operationMapping = biMapOf(
        0x00uL to Operation.CLEAR,
        0x01uL to Operation.LOAD,
        0x02uL to Operation.AND,
        0x03uL to Operation.NAND,
        0x04uL to Operation.OR,
        0x05uL to Operation.NOR,
        0x06uL to Operation.XOR,
        0x07uL to Operation.XNOR,
        0x08uL to Operation.INCREMENT,
        0x09uL to Operation.DECREMENT,
        0x0AuL to Operation.ADD,
        0x0BuL to Operation.SUBTRACT,
        0x0CuL to Operation.SHIFT_LEFT,
        0x0DuL to Operation.SHIFT_RIGHT,
        0x0EuL to Operation.ROTATE_LEFT,
        0x0FuL to Operation.ROTATE_RIGHT,
        0x10uL to Operation.MEMORY_LOAD,
        0x11uL to Operation.MEMORY_STORE,
        0x12uL to Operation.IO_POLL,
        0x13uL to Operation.IO_READ,
        0x14uL to Operation.IO_WRITE,
        0x15uL to Operation.STACK_PEEK,
        0x16uL to Operation.CALL,
        0x17uL to Operation.STACK_PUSH,
        0x18uL to Operation.STACK_POP,
        0x19uL to Operation.BIT_GET,
        0x1AuL to Operation.BIT_SET,
        0x1BuL to Operation.BIT_CLEAR,
        0x1CuL to Operation.BIT_TOGGLE,
        0x1DuL to Operation.ADD_WITH_CARRY,
        0x1EuL to Operation.SUBTRACT_WITH_CARRY,
        0x30uL to Operation.MULTIPLY,
        0x1DuL to Operation.RETURN,
        0x3FuL to Operation.BREAK,
    )
}
