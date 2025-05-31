package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.RegisterLike

public class ReplaceSpecialRegistersStep(public val architecture: Architecture<*, *>) : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Transform program counter.
        program.transformArgumentsOfType<RegisterLike.PC> { statementInstance, parameter, value ->
            RegisterLike.RegisterReference(architecture.registers.programCounterVariable)
        }

        // Success.
        return Result.success(Unit)
    }
}
