package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.anchor.Label
import net.voxelpi.axiom.asm.variable.Variable
import java.util.UUID

public class GlobalScope(
    override val scopes: MutableList<Scope> = mutableListOf(),
    override val variables: MutableMap<String, Variable> = mutableMapOf(),
    override val labels: MutableMap<String, Label> = mutableMapOf(),
) : Scope {

    // The global scope has a unique id of 0.
    override val uniqueId: UUID = UUID(0, 0)

    override fun isLabelDefined(name: String): Boolean {
        return labels.containsKey(name) || isLabelDefinedByChild(name)
    }

    override fun isVariableDefined(name: String): Boolean {
        return variables.containsKey(name)
    }

    override fun findLabel(name: String): Pair<Label, Scope>? {
        if (labels.containsKey(name)) {
            return labels[name]!! to this
        }
        return findLabelInChild(name)
    }

    override fun findVariable(name: String): Pair<Variable, Scope>? {
        return variables[name]?.let { it to this }
    }

    override fun findVariable(uniqueId: UUID): Pair<Variable, Scope>? {
        return variables.values.find { it.uniqueId == uniqueId }?.let { it to this }
    }
}
