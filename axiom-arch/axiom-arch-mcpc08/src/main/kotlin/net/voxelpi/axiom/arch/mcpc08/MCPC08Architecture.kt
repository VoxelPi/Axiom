package net.voxelpi.axiom.arch.mcpc08

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.register.RegisterFile
import net.voxelpi.axiom.util.biMapOf

public object MCPC08Architecture : Architecture(
    "mcpc8",
    WordType.INT16,
    WordType.INT8,
    256,
    WordType.INT8,
    256,
    WordType.INT8,
) {

    override val registers: RegisterFile = RegisterFile.create("PC", WordType.INT8) {
        programCounterVariable = createVariable("PC", programCounter, 0, readable = true, writeable = true, conditionable = false)

        for (registerIndex in 1..7) {
            val isConditional = registerIndex == 7
            val conditionSourceIndex = 1

            val register = createRegister("R$registerIndex", WordType.INT8)
            createVariable("R$registerIndex", register, registerIndex, readable = true, writeable = true, conditionable = isConditional)
            if (isConditional) {
                createVariable("C$conditionSourceIndex", register, registerIndex, readable = true, writeable = true, conditionable = true)
            }
        }
    }

    override val supportedOperations: Set<Operation>
        get() = operationMapping.values

    override fun encodeInstruction(instruction: Instruction): Result<UByteArray> {
        var encodedInstruction: UShort = 0u

        val a = instruction.inputA
        val b = instruction.inputB

        if (instruction.operation == Operation.LOAD) {
            // Encode value.
            if (a !is InstructionValue.ImmediateValue) {
                return Result.failure(IllegalArgumentException("Input A for LOAD must be an immediate value."))
            }
            encodedInstruction = encodedInstruction or (a.value and 0xFF).toUShort()

            // Encode unset relative flag.
            encodedInstruction = encodedInstruction or (0u shl 8).toUShort()

            // Encode condition.
            encodedInstruction = encodedInstruction or ((conditionMapping.inverse[instruction.condition]!! and 0b111u) shl 9).toUShort()

            // Check condition register.
            if (instruction.conditionRegister.id != "C1") {
                return Result.failure(IllegalArgumentException("Condition register for LOAD must be C1."))
            }

            // Encode output register address.
            encodedInstruction = encodedInstruction or ((instruction.outputRegister.address and 0b111) shl 12).toUShort()

            // Encode unset mode flag
            encodedInstruction = encodedInstruction or (0u shl 15).toUShort()
        } else if (instruction.operation == Operation.ADD && b is InstructionValue.ImmediateValue && a is InstructionValue.RegisterReference && a.register.id == instruction.outputRegister.id) {
            // Encode value.
            encodedInstruction = encodedInstruction or (b.value and 0xFF).toUShort()

            // Encode unset relative flag.
            encodedInstruction = encodedInstruction or (1u shl 8).toUShort()

            // Encode condition.
            encodedInstruction = encodedInstruction or ((conditionMapping.inverse[instruction.condition]!! and 0b111u) shl 9).toUShort()

            // Check condition register.
            if (instruction.conditionRegister.id != "C1") {
                return Result.failure(IllegalArgumentException("Condition register for LOAD must be C1."))
            }

            // Encode output register address.
            encodedInstruction = encodedInstruction or ((instruction.outputRegister.address and 0b111) shl 12).toUShort()

            // Encode unset mode flag
            encodedInstruction = encodedInstruction or (0u shl 15).toUShort()
        } else {
            // Encode operation.
            encodedInstruction = encodedInstruction or ((operationMapping.inverse[instruction.operation]!! and 0b111111u) shl 0).toUShort()

            // Encode register a.
            if (a !is InstructionValue.RegisterReference) {
                return Result.failure(IllegalArgumentException("Input A for ${instruction.operation} must be a register."))
            }
            encodedInstruction = encodedInstruction or ((a.register.address and 0b111) shl 6).toUShort()

            // Encode register b.
            if (b !is InstructionValue.RegisterReference) {
                return Result.failure(IllegalArgumentException("Input B for ${instruction.operation} must be a register."))
            }
            encodedInstruction = encodedInstruction or ((b.register.address and 0b111) shl 9).toUShort()

            // Encode output register address.
            encodedInstruction = encodedInstruction or ((instruction.outputRegister.address and 0b111) shl 12).toUShort()

            // Encode set mode flag
            encodedInstruction = encodedInstruction or (1u shl 15).toUShort()
        }
        return Result.success(WordType.INT16.pack(encodedInstruction.toULong()))
    }

    override fun decodeInstruction(encodedInstruction: UByteArray): Result<Instruction> {
        val encodedInstruction = WordType.INT16.unpack(encodedInstruction)

        val mode = (encodedInstruction.toUInt() shr 15) and 1u != 0u

        // Decode the output register.
        val outputRegisterAddress = ((encodedInstruction.toUInt() shr 12) and 0b111u).toInt()
        val outputRegister = registers.variables.values.find { it.address == outputRegisterAddress && it.writable }
            ?: return Result.failure(IllegalArgumentException("Invalid output register address $outputRegisterAddress"))

        val conditionRegister = registers.variable("C1")!!

        return if (mode) {
            // Decode the value.
            val value = InstructionValue.ImmediateValue((encodedInstruction and 0xFFu).toLong())

            // Decode the relative flag.
            val relativeFlag = (encodedInstruction.toUInt() shr 8) and 1u != 0u

            // Decode the condition.
            val condition = conditionMapping[((encodedInstruction.toUInt() shr 9) and 0b111u)]
                ?: return Result.failure(IllegalArgumentException("Invalid condition ${((encodedInstruction.toUInt() shr 9) and 0b111u)}"))

            if (relativeFlag) {
                Result.success(
                    Instruction(
                        Operation.ADD,
                        condition,
                        conditionRegister,
                        outputRegister,
                        InstructionValue.RegisterReference(outputRegister),
                        value,
                        emptyMap(),
                    )
                )
            } else {
                Result.success(
                    Instruction(
                        Operation.LOAD,
                        condition,
                        conditionRegister,
                        outputRegister,
                        value,
                        InstructionValue.ImmediateValue(0),
                        emptyMap(),
                    )
                )
            }
        } else {
            // Decode operation.
            val operation = operationMapping[((encodedInstruction.toUInt() shr 0) and 0b111111u)]!!

            // Decode the a-register.
            val registerAddressA = ((encodedInstruction.toUInt() shr 6) and 0b111u).toInt()
            val registerA = registers.variables.values.find { it.address == registerAddressA && it.readable }
                ?: return Result.failure(IllegalArgumentException("Invalid input A register address $registerAddressA"))
            InstructionValue.RegisterReference(registerA)

            // Decode the b-register.
            val registerAddressB = ((encodedInstruction.toUInt() shr 9) and 0b111u).toInt()
            val registerB = registers.variables.values.find { it.address == registerAddressB && it.readable }
                ?: return Result.failure(IllegalArgumentException("Invalid input B register address $registerAddressB"))
            InstructionValue.RegisterReference(registerB)

            Result.success(
                Instruction(
                    operation,
                    Condition.ALWAYS,
                    conditionRegister,
                    outputRegister,
                    InstructionValue.RegisterReference(registerA),
                    InstructionValue.RegisterReference(registerB),
                    emptyMap(),
                )
            )
        }
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
        0x00u to Operation.CLEAR,
        0x01u to Operation.LOAD,
        0x02u to Operation.AND,
        0x03u to Operation.NAND,
        0x04u to Operation.OR,
        0x05u to Operation.NOR,
        0x06u to Operation.XOR,
        0x07u to Operation.XNOR,
        0x08u to Operation.INCREMENT,
        0x09u to Operation.DECREMENT,
        0x0Au to Operation.ADD,
        0x0Bu to Operation.SUBTRACT,
        0x0Cu to Operation.SHIFT_LEFT,
        0x0Du to Operation.SHIFT_RIGHT,
        0x0Eu to Operation.ROTATE_LEFT,
        0x0Fu to Operation.ROTATE_RIGHT,
        0x18u to Operation.MEMORY_LOAD,
        0x19u to Operation.MEMORY_STORE,
        0x1Au to Operation.IO_POLL,
        0x1Bu to Operation.IO_READ,
        0x1Cu to Operation.IO_WRITE,
        0x1Du to Operation.STACK_PUSH,
        0x1Eu to Operation.STACK_PEEK,
        0x1Fu to Operation.STACK_POP,
        0x20u to Operation.BIT_GET,
        0x21u to Operation.BIT_SET,
        0x22u to Operation.BIT_CLEAR,
        0x23u to Operation.BIT_TOGGLE,
        0x30u to Operation.MULTIPLY,
        0x31u to Operation.DIVIDE,
        0x32u to Operation.MODULO,
        0x3Fu to Operation.BREAK,
    )
}
