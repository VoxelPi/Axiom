package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.asm.Assembler.Companion.START_LABEL_NAME
import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement

public object DefineImplicitStartLabelStep : ProgramPipelineStep {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Define the start label if it is not yet defined
        if (!program.globalScope.isLabelDefined(START_LABEL_NAME)) {
            val label = program.globalScope.defineLabel(START_LABEL_NAME).getOrElse {
                return Result.failure(CompilationException("Unable to define implicit start label. ${it.message}"))
            }
            program.anchors[label.uniqueId] = label

            // Create the corresponding anchor statement.
            program.statements.add(0, ANCHOR_STATEMENT_PROTOTYPE.createInstance(AnchorStatement(label), program.globalScope, SourceLink.Generated("@start", "implicit start label")))
        }

        return Result.success(Unit)
    }

    private val ANCHOR_STATEMENT_PROTOTYPE = StatementPrototype.fromType<AnchorStatement>().getOrThrow()
}
