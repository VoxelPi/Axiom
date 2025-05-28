package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.VariableLike

public object ResolveVariableValuesStep : ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        for (scope in program.sortedScopes()) {
            val scopeVariables = scope.variables.values.toList()
            for (variableDefinition in scopeVariables) {
                var value = variableDefinition.value
                val variableTrace = mutableSetOf(variableDefinition.uniqueId)
                while (value is VariableLike.VariableReference) {
                    if (value.id in variableTrace) {
                        return Result.failure(CompilationException("Circular reference detected for variable \"${value.id}\"."))
                    }
                    val (referencedVariable, _) = scope.findVariable(value.id)
                        ?: return Result.failure(CompilationException("Unknown variable \"${value.id}\"."))
                    variableTrace.add(referencedVariable.uniqueId)
                    value = referencedVariable.value
                }
                scope.updateVariable(variableDefinition.name, value)
            }
        }

        return program.transformArgumentsOfType<VariableLike.VariableReference> { statementInstance, parameter, value ->
            val (variable, _) = statementInstance.scope.findVariable(value.id)
                ?: throw SourceCompilationException(statementInstance.sourceOfOrDefault(parameter), "Unknown variable \"${value.id}\".")
            variable.value
        }
    }
}
