package net.voxelpi.axiom.asm.pipeline

import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram

public interface ProgramPipelineStep<T> {
    public fun transform(program: MutableStatementProgram): Result<T>
}
