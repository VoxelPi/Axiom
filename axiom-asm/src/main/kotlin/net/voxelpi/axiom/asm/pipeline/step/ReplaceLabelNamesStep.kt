package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.AnchorLike
import net.voxelpi.axiom.asm.type.LabelLike

public object ReplaceLabelNamesStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Replace label names in statements.
        program.transformArgumentsOfType<LabelLike.LabelName> { statementInstance, parameter, value ->
            val (label, _) = statementInstance.scope.findLabel(value.name)
                ?: throw SourceCompilationException(statementInstance.sourceOfOrDefault(parameter), "Unknown label \"${value.name}\".")
            AnchorLike.AnchorReference(label)
        }.getOrElse { return Result.failure(it) }

        // Replace label names in variables.
        for (scope in program.sortedScopes()) {
            val scopeVariables = scope.variables.values.toList()
            for (variableDefinition in scopeVariables) {
                var value = variableDefinition.value
                if (value is LabelLike.LabelName) {
                    val (label, _) = scope.findLabel(value.name)
                        ?: return Result.failure(CompilationException("Unknown label \"${value.name}\"."))
                    value = AnchorLike.AnchorReference(label)
                }
                scope.updateVariable(variableDefinition.name, value)
            }
        }

        return Result.success(Unit)
    }
}
