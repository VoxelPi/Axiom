package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.ScopeLike

public object ReplaceScopeNamesStep : ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        program.transformArgumentsOfType<ScopeLike.ScopeName> { statementInstance, parameter, value ->
            val scope = statementInstance.scope.findScope(value.name)
                ?: throw SourceCompilationException(statementInstance.sourceOfOrDefault(parameter), "Unknown scope \"${value.name}\".")
            ScopeLike.ScopeReference(scope)
        }.getOrElse { return Result.failure(it) }
        program.transformArgumentsOfType<ScopeLike.ParentScope> { statementInstance, parameter, value ->
            ScopeLike.ScopeReference(statementInstance.scope)
        }.getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }
}
