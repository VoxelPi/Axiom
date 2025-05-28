package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.VariableLike

public object ReplaceVariableNamesStep : ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Transform variable names.
        program.transformArgumentsOfType<VariableLike.VariableName> { statementInstance, parameter, value ->
            val (variable, _) = statementInstance.scope.findVariable(value.name)
                ?: throw IllegalArgumentException("Unknown variable \"${value.name}\".")
            VariableLike.VariableReference(variable.uniqueId)
        }.getOrElse { return Result.failure(it) }

        for (scope in program.sortedScopes()) {
            val scopeVariables = scope.variables.values.toList()
            for (variableDefinition in scopeVariables) {
                var value = variableDefinition.value
                if (value is VariableLike.VariableName) {
                    val (referencedVariable, _) = scope.findVariable(value.name)
                        ?: return Result.failure(CompilationException("Unknown variable \"${value.name}\"."))
                    value = VariableLike.VariableReference(referencedVariable.uniqueId)
                }
                scope.updateVariable(variableDefinition.name, value)
            }
        }

        return Result.success(Unit)
    }
}
