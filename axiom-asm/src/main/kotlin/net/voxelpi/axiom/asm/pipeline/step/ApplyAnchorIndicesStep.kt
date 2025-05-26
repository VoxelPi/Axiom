package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.AnchorLike
import net.voxelpi.axiom.asm.type.IntegerValue
import java.util.UUID

public class ApplyAnchorIndicesStep(public val anchorIndices: Map<UUID, Int>): ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformArgumentsOfType<AnchorLike.AnchorReference> { anchorReference ->
            IntegerValue(anchorIndices[anchorReference.anchor.uniqueId]!!.toLong())
        }
    }
}
