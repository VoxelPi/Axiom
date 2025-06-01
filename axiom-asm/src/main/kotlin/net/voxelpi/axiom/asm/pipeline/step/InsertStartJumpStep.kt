package net.voxelpi.axiom.asm.pipeline.step

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.Assembler.Companion.START_LABEL_NAME
import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.pipeline.ProgramPipelineStep
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.type.AnchorLike
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public class InsertStartJumpStep(
    public val architecture: Architecture,
) : ProgramPipelineStep<Unit> {

    override fun transform(program: MutableStatementProgram): Result<Unit> {
        // Calculate the start index.
        val (startAnchor, startAnchorIndex) = findStartLabel(program).getOrElse { return Result.failure(it) }

        // If the start index is 0, no jump is needed.
        if (startAnchorIndex == 0) {
            return Result.success(Unit)
        }

        // Create a jump statement to the @global:start label and insert it at the beginning of the program.
        program.statements.add(
            0,
            INSTRUCTION_STATEMENT_PROTOTYPE.createInstance(
                InstructionStatement.WithOutput(
                    Operation.LOAD,
                    Condition.ALWAYS,
                    RegisterLike.AnyRegister(conditionable = true),
                    AnchorLike.AnchorReference(startAnchor),
                    IntegerValue(0),
                    RegisterLike.RegisterReference(architecture.registers.programCounterVariable),
                ),
                program.globalScope,
                SourceLink.Generated("jump @${START_LABEL_NAME}", "implicit start jump"),
            )
        )

        return Result.success(Unit)
    }

    private fun findStartLabel(program: MutableStatementProgram): Result<Pair<Anchor.Named, Int>> {
        var iStartIndex = 0
        var startAnchor: Anchor.Named? = null
        for (statementInstance in program.statements) {
            val statement = statementInstance.create()
            when (statement) {
                is InstructionStatement -> {
                    iStartIndex += 1
                }
                is AnchorStatement -> {
                    val anchor = statement.anchor
                    if (anchor is Anchor.Named && anchor.name == START_LABEL_NAME) {
                        startAnchor = anchor
                        break
                    }
                }
                else -> {}
            }
        }

        if (startAnchor == null) {
            return Result.failure(IllegalStateException("Could not find start anchor."))
        }
        return Result.success(startAnchor to iStartIndex)
    }

    public companion object {
        private val INSTRUCTION_STATEMENT_PROTOTYPE = StatementPrototype.fromType<InstructionStatement.WithOutput>().getOrThrow()
    }
}
