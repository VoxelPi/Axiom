package net.voxelpi.axiom.register

import net.voxelpi.axiom.WordType

public class RegisterFile<P : Comparable<P>>(
    public val registers: Map<String, Register<*>>,
    public val variables: Map<String, RegisterVariable<*, *>>,
    public val programCounter: Register<P>,
    public val programCounterVariable: RegisterVariable.Direct<P>,
) {

    public fun registers(): Collection<Register<*>> {
        return registers.values
    }

    public fun register(id: String): Register<*>? {
        return registers[id]
    }

    public fun variables(): Collection<RegisterVariable<*, *>> {
        return variables.values
    }

    public fun variable(id: String): RegisterVariable<*, *>? {
        return variables[id]
    }

    public companion object {
        public fun <P : Comparable<P>> create(
            programCounterId: String,
            programCounterType: WordType<P>,
            block: Builder<P>.() -> Unit,
        ): RegisterFile<P> {
            val builder = Builder(programCounterId, programCounterType)
            builder.block()
            return builder.build()
        }
    }

    public class Builder<P : Comparable<P>> internal constructor(
        programCounterId: String,
        programCounterType: WordType<P>,
    ) {
        public val programCounter: Register<P> = Register(programCounterId, programCounterType)
        public lateinit var programCounterVariable: RegisterVariable.Direct<P>

        private var registers: MutableMap<String, Register<*>> = mutableMapOf(programCounter.id to programCounter)
        private var variables: MutableMap<String, RegisterVariable<*, *>> = mutableMapOf()

        public fun createRegister(id: String, type: WordType<*>): Register<*> {
            val register = Register(id, type)
            registers[id] = register
            return register
        }

        public fun <R : Comparable<R>> createVariable(
            id: String,
            register: Register<R>,
            address: Int,
            readable: Boolean,
            writeable: Boolean,
            conditionable: Boolean,
        ): RegisterVariable.Direct<R> {
            val variable = RegisterVariable.Direct(id, register, address, readable, writeable, conditionable)
            variables[id] = variable
            return variable
        }

        public fun <V : Comparable<V>, R : Comparable<R>> createVariable(
            id: String,
            register: Register<R>,
            type: WordType<V>,
            part: Int,
            address: Int,
            readable: Boolean,
            writeable: Boolean,
            conditionable: Boolean,
        ): RegisterVariable.Part<V, R> {
            val variable = RegisterVariable.Part(id, type, register, part, address, readable, writeable, conditionable)
            variables[id] = variable
            return variable
        }

        internal fun build(): RegisterFile<P> {
            return RegisterFile(
                registers,
                variables,
                programCounter,
                programCounterVariable,
            )
        }
    }
}
