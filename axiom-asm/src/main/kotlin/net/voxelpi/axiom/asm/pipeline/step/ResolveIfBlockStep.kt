package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.IfStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.type.AnchorLike
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.instruction.Operation
import java.util.UUID

public object ResolveIfBlockStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        for (iStatement in program.statements.indices) {
            val statementInstance = program.statements[iStatement]
            val statement = statementInstance.create()
            if (statement !is IfStatement) {
                continue
            }

            // Check that the if statement is not the last statement.
            if (iStatement == program.statements.size - 1) {
                return Result.failure(SourceCompilationException(statementInstance.source, "If statement must be followed by a scope."))
            }

            val followingStatementInstance = program.statements[iStatement + 1]
            val followingStatement = followingStatementInstance.create()
            when (followingStatement) {
                is AnchorStatement -> {
                    val anchor = followingStatement.anchor
                    if (anchor !is ScopeAnchor.ScopeStart) {
                        return Result.failure(SourceCompilationException(statementInstance.source, "If statement must be followed by a scope or an instruction."))
                    }

                    // Replace the if-statement with a conditional jump statement to the end of the scope.
                    program.statements[iStatement] = JUMP_STATEMENT_PROTOTYPE.createInstance(
                        InstructionStatement.WithOutput(
                            Operation.LOAD,
                            statement.condition.inv(),
                            statement.conditionValue,
                            AnchorLike.AnchorReference(anchor.scope.endAnchor),
                            IntegerValue(0),
                            RegisterLike.PC,
                        ),
                        statementInstance.scope,
                        statementInstance.source,
                    )
                }
                is InstructionStatement -> {
                    // Create an unnamed anchor after the next instruction.
                    val anchor = Anchor.Unnamed(UUID.randomUUID())
                    program.statements.add(
                        iStatement + 2,
                        ANCHOR_STATEMENT_PROTOTYPE.createInstance(AnchorStatement(anchor), statementInstance.scope, statementInstance.source),
                    )

                    // Replace the if-statement with a conditional jump statement to the previously created anchor.
                    program.statements[iStatement] = JUMP_STATEMENT_PROTOTYPE.createInstance(
                        InstructionStatement.WithOutput(
                            Operation.LOAD,
                            statement.condition.inv(),
                            statement.conditionValue,
                            AnchorLike.AnchorReference(anchor),
                            IntegerValue(0),
                            RegisterLike.PC,
                        ),
                        statementInstance.scope,
                        statementInstance.source,
                    )
                }
                else -> {
                    return Result.failure(SourceCompilationException(statementInstance.source, "If statement must be followed by a scope or an instruction."))
                }
            }
        }
        return Result.success(Unit)
    }

    private val ANCHOR_STATEMENT_PROTOTYPE = StatementPrototype.fromType<AnchorStatement>().getOrThrow()
    private val JUMP_STATEMENT_PROTOTYPE = StatementPrototype.fromType<InstructionStatement.WithOutput>().getOrThrow()
}
