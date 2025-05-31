package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.statement.types.ScopeJumpStatement
import net.voxelpi.axiom.asm.type.AnchorLike
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.instruction.Operation

public object ResolveScopeJumpStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformType<ScopeJumpStatement> { statementInstance ->
            val statement = statementInstance.create()

            val scopeReference = statement.scope
            if (scopeReference !is ScopeLike.ScopeReference) {
                throw SourceCompilationException(statementInstance.sourceOfOrDefault(ScopeJumpStatement::scope), "Scope reference is not resolved.")
            }
            val scope = scopeReference.scope

            if (scope !is LocalScope) {
                throw SourceCompilationException(statementInstance.sourceOfOrDefault(ScopeJumpStatement::scope), "Scope reference is not a local scope.")
            }

            when (statement) {
                is ScopeJumpStatement.Exit -> {
                    yield(
                        JUMP_STATEMENT_PROTOTYPE.createInstance(
                            InstructionStatement.WithOutput(
                                Operation.LOAD,
                                statement.condition,
                                statement.conditionValue,
                                AnchorLike.AnchorReference(scope.endAnchor),
                                IntegerValue(0),
                                RegisterLike.PC,
                            ),
                            statementInstance.scope,
                            statementInstance.source,
                        )
                    )
                }
                is ScopeJumpStatement.Repeat -> {
                    yield(
                        JUMP_STATEMENT_PROTOTYPE.createInstance(
                            InstructionStatement.WithOutput(
                                Operation.LOAD,
                                statement.condition,
                                statement.conditionValue,
                                AnchorLike.AnchorReference(scope.startAnchor),
                                IntegerValue(0),
                                RegisterLike.PC,
                            ),
                            statementInstance.scope,
                            statementInstance.source,
                        )
                    )
                }
            }
        }
    }

    private val JUMP_STATEMENT_PROTOTYPE = StatementPrototype.fromType<InstructionStatement.WithOutput>().getOrThrow()
}
