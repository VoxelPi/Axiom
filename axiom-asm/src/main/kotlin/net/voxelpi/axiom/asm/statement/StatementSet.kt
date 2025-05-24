package net.voxelpi.axiom.asm.statement

public data class StatementSet(
    override val id: String,
    override val parameters: Map<String, StatementParameter<*>>,
    override val sets: Set<StatementSet>,
) : StatementSetElement {

    public companion object {

        public fun create(id: String, parameters: Iterable<StatementParameter<*>>, sets: Set<StatementSet> = emptySet()): StatementSet {
            val parameterMap = parameters.associateBy { it.id }.toMutableMap()
            for (set in sets) {
                parameterMap += set.parameters
            }

            return StatementSet(id, parameterMap, sets)
        }

        public fun create(id: String, vararg sets: StatementSet, block: Builder.() -> Unit = {}): StatementSet {
            val builder = Builder(id, sets = sets.toMutableSet())
            builder.block()
            return StatementSet(builder.id, builder.parameters, builder.sets)
        }
    }

    public class Builder internal constructor(
        public val id: String,
        public val parameters: MutableMap<String, StatementParameter<*>> = mutableMapOf(),
        public val sets: MutableSet<StatementSet> = mutableSetOf(),
    ) {

        public fun declare(parameter: StatementParameter<*>) {
            parameters[parameter.id] = parameter
        }
    }
}
