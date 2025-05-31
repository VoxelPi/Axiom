package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.VariableStatement

public object DefineVariablesStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformType<VariableStatement.Definition> { statementInstance ->
            val statement = statementInstance.create()
            statementInstance.scope.defineVariable(statement.name.name, statement.value).getOrElse {
                throw SourceCompilationException(statementInstance.source, it.message ?: "")
            }
        }
    }
}
