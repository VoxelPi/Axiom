package net.voxelpi.axiom.asm.pipeline

import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram

public class ProgramPipeline(
    public val steps: List<ProgramPipelineStep<*>>,
) : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        for (step in steps) {
            step.transform(program).onFailure {
                return Result.failure(it)
            }
        }
        return Result.success(Unit)
    }
}
