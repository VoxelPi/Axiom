package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.LabelStatement

public object DefineLabelsStep : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        return program.transformType<LabelStatement.Definition> { statementInstance ->
            val statement = statementInstance.create()
            val label = statementInstance.scope.defineLabel(statement.name.name).getOrElse {
                throw SourceCompilationException(statementInstance.source, it.message ?: "")
            }
            program.anchors[label.uniqueId] = label

            // Create the corresponding anchor statement.
            yield(ANCHOR_STATEMENT_PROTOTYPE.createInstance(AnchorStatement(label), statementInstance.scope, statementInstance.source))
        }
    }

    private val ANCHOR_STATEMENT_PROTOTYPE = StatementPrototype.fromType<AnchorStatement>().getOrThrow()
}
