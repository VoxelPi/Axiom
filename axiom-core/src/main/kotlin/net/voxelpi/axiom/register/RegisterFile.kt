package net.voxelpi.axiom.register

import net.voxelpi.axiom.WordType

public class RegisterFile(
    public val registers: Map<String, Register>,
    public val variables: Map<String, RegisterVariable>,
    public val programCounter: Register,
    public val programCounterVariable: RegisterVariable.Direct,
    public val indexRegister: Register?,
) {

    public fun registers(): Collection<Register> {
        return registers.values
    }

    public fun register(id: String): Register? {
        return registers[id]
    }

    public fun variables(): Collection<RegisterVariable> {
        return variables.values
    }

    public fun variable(id: String): RegisterVariable? {
        return variables[id]
    }

    public companion object {
        public fun create(
            programCounterId: String,
            programCounterType: WordType,
            block: Builder.() -> Unit,
        ): RegisterFile {
            val builder = Builder(programCounterId, programCounterType)
            builder.block()
            return builder.build()
        }
    }

    public class Builder internal constructor(
        programCounterId: String,
        programCounterType: WordType,
    ) {
        public val programCounter: Register = Register(programCounterId, programCounterType)
        public lateinit var programCounterVariable: RegisterVariable.Direct
        public var indexRegister: Register? = null

        private var registers: MutableMap<String, Register> = mutableMapOf(programCounter.id to programCounter)
        private var variables: MutableMap<String, RegisterVariable> = mutableMapOf()

        public fun createRegister(id: String, type: WordType): Register {
            val register = Register(id, type)
            registers[id] = register
            return register
        }

        public fun createVariable(
            id: String,
            register: Register,
            address: Int,
            readable: Boolean = false,
            writeable: Boolean = false,
            conditionable: Boolean = false,
            needsMode2: Boolean = false,
        ): RegisterVariable.Direct {
            val variable = RegisterVariable.Direct(id, register, address, readable, writeable, conditionable, needsMode2)
            variables[id] = variable
            return variable
        }

        public fun createVariable(
            id: String,
            register: Register,
            type: WordType,
            part: Int,
            address: Int,
            readable: Boolean = false,
            writeable: Boolean = false,
            conditionable: Boolean = false,
            needsMode2: Boolean = false,
        ): RegisterVariable.Part {
            val variable = RegisterVariable.Part(id, type, register, part, address, readable, writeable, conditionable, needsMode2)
            variables[id] = variable
            return variable
        }

        internal fun build(): RegisterFile {
            return RegisterFile(
                registers,
                variables,
                programCounter,
                programCounterVariable,
                indexRegister,
            )
        }
    }
}
