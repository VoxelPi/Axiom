package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import java.util.UUID

public class ApplyAnchorIndicesStep(public val anchorIndices: Map<UUID, Int>): ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transform { previousStatementInstance ->
            yield(previousStatementInstance)
        }
    }
}
