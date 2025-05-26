package net.voxelpi.axiom.asm.pipeline

import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram

public interface ProgramPipelineStep {
    public fun transform(program: MutableStatementProgram) : Result<Unit>
}
