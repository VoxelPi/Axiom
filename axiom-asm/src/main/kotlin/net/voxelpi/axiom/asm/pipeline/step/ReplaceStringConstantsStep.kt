package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.ConstantStatement
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.StringLike

public object ReplaceStringConstantsStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformType<ConstantStatement.StringConstant> { statementInstance ->
            val value = statementInstance[ConstantStatement.StringConstant::value]
            if (value !is StringLike.StringValue) {
                throw SourceCompilationException(statementInstance.source, "Unresolved string constant")
            }

            val text = value.value

            for (c in text) {
                val symbolValue = c.code
                yield(
                    INTEGER_CONSTANT_PROTOTYPE.createInstance(
                        ConstantStatement.IntegerConstant(
                            IntegerValue(symbolValue.toLong())
                        ),
                        statementInstance.scope,
                        statementInstance.source,
                    )
                )
            }
            // Yield terminating 0.
            yield(
                INTEGER_CONSTANT_PROTOTYPE.createInstance(
                    ConstantStatement.IntegerConstant(
                        IntegerValue(0)
                    ),
                    statementInstance.scope,
                    statementInstance.source,
                )
            )
        }
    }

    private val INTEGER_CONSTANT_PROTOTYPE = StatementPrototype.fromType<ConstantStatement.IntegerConstant>().getOrThrow()
}
