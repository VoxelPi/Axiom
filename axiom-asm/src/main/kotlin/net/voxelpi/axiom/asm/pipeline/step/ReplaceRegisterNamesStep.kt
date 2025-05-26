package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike

public class ReplaceRegisterNamesStep(public val architecture: Architecture<*>) : ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Transform register names.
        program.transformArgumentsOfType<RegisterLike.RegisterName> { value, source ->
            val register = architecture.registers.variable(value.name)
                ?: throw SourceCompilationException(source, "Unknown register \"${value.name}\". Available registers: ${architecture.registers.variables().map { it.id }}.")
            RegisterLike.RegisterReference(register)
        }.getOrElse { return Result.failure(it) }

        // Transform unparsed values.
        program.transformArgumentsOfType<ValueLike.UnparsedValue> { value, _ ->
            architecture.registers.variable(value.value)?.let { RegisterLike.RegisterReference(it) } ?: value
        }.getOrElse { return Result.failure(it) }

        // Success.
        return Result.success(Unit)
    }
}
