package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.AnchorLike
import net.voxelpi.axiom.asm.type.IntegerValue
import java.util.UUID

public class ApplyAnchorIndicesStep(public val anchorIndices: Map<UUID, Int>) : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformArgumentsOfType<AnchorLike.AnchorReference> { statementInstance, parameter, anchorReference ->
            val source = statementInstance.sourceOfOrDefault(parameter)
            val anchorIndex = anchorIndices[anchorReference.anchor.uniqueId]
                ?: throw SourceCompilationException(source, "Unknown anchor ${anchorReference.anchor.uniqueId}")
            IntegerValue(anchorIndex.toLong())
        }
    }
}
