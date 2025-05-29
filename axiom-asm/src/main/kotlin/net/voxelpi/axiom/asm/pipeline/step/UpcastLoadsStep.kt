package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.WordType
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Operation

public class UpcastLoadsStep(
    public val architecture: Architecture<*, *>,
) : ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformType<InstructionStatement.WithOutput> { statementInstance ->
            val statement = statementInstance.create()

            val output = statement.output
            if (output !is RegisterLike.RegisterReference) {
                throw SourceCompilationException(statementInstance.sourceOfOrDefault(InstructionStatement.WithOutput::output), "Output is not a register reference but is instead ${output}.")
            }
            val outputRegister = output.register

            if (outputRegister.type <= architecture.dataWordType) {
                yield(statementInstance)
                return@transformType
            }

            when (statement.operation) {
                Operation.LOAD -> {
                    val (a, b) = splitValue(
                        statement.inputA,
                        statementInstance.sourceOfOrDefault(InstructionStatement.WithOutput::inputA),
                        architecture.dataWordType,
                    )

                    yield(
                        statementInstance.modifiedCopy {
                            this[InstructionStatement::operation] = Operation.LOAD_2
                            this[InstructionStatement::inputA] = a
                            this[InstructionStatement::inputB] = b
                        }
                    )
                }
                Operation.CALL -> {
                    val (a, b) = splitValue(
                        statement.inputA,
                        statementInstance.sourceOfOrDefault(InstructionStatement.WithOutput::inputA),
                        architecture.dataWordType,
                    )

                    yield(
                        statementInstance.modifiedCopy {
                            this[InstructionStatement::operation] = Operation.CALL_2
                            this[InstructionStatement::inputA] = a
                            this[InstructionStatement::inputB] = b
                        }
                    )
                }
                else -> {
                    yield(statementInstance)
                }
            }
        }
    }

    private fun splitValue(value: ValueLike, source: SourceLink, type: WordType<*>): Pair<ValueLike, ValueLike> {
        return when (value) {
            is IntegerValue -> {
                val integer = value.value.toULong()
                val partA = integer and type.mask
                val partB = (integer shr type.bits) and type.mask
                Pair(IntegerValue(partA.toLong()), IntegerValue(partB.toLong()))
            }
            is RegisterLike.RegisterReference -> {
                Pair(value, IntegerValue(0))
            }
            else -> throw SourceCompilationException(source, "Input is not a register reference or integer value.")
        }
    }
}
