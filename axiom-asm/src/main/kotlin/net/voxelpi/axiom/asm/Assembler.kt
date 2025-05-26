package net.voxelpi.axiom.asm

import net.voxelpi.axiom.ImmediateValue
import net.voxelpi.axiom.Register
import net.voxelpi.axiom.ValueProvider
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.parser.Parsers
import net.voxelpi.axiom.asm.pipeline.step.ApplyAnchorIndicesStep
import net.voxelpi.axiom.asm.pipeline.step.DefineImplicitStartLabelStep
import net.voxelpi.axiom.asm.pipeline.step.DefineLabelsStep
import net.voxelpi.axiom.asm.pipeline.step.DefineVariablesStep
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.program.StatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.Program
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile

public class Assembler(
    public val includeDirectories: List<Path>,
) {

    public fun assemble(text: String, architecture: Architecture<*>): Result<Program> {
        val compilationUnit = CompilationUnit("__provided__", text)
        return assemble(compilationUnit, architecture)
    }

    public fun assemble(path: Path, architecture: Architecture<*>): Result<Program> {
        if (!path.isRegularFile()) {
            return Result.failure(IllegalArgumentException("The path $path is not a regular file."))
        }

        val compilationUnit = CompilationUnit(path.normalize().absolutePathString(), path.toFile().readText())
        return assemble(compilationUnit, architecture)
    }

    public fun assemble(unit: CompilationUnit, architecture: Architecture<*>, parser: Parser = Parsers.AXIOM_ASM): Result<Program> = runCatching {
        val unitCollector = CompilationUnitCollector.create(unit, parser, includeDirectories).getOrThrow()
        val program = unitCollector.reduce().getOrThrow()

        // Define variables.
        DefineVariablesStep.transform(program).getOrThrow()

        // Define labels.
        DefineLabelsStep.transform(program).getOrThrow()
        DefineImplicitStartLabelStep.transform(program).getOrThrow()

        // TODO: Replace register names

        // TODO: Resolve variables

        // TODO: Insert start jump

        // Generate anchors indices and use them for all anchor reference parameters.
        val anchorIndices = generateAnchorIndices(program).getOrThrow()
        ApplyAnchorIndicesStep(anchorIndices).transform(program).getOrThrow()

        val instructions = generateInstructions(program, architecture).getOrThrow()
        val compiledProgram = Program(instructions)
        return Result.success(compiledProgram)
    }

    private fun generateAnchorIndices(program: MutableStatementProgram): Result<Map<UUID, Int>> {
        val anchorIndices = mutableMapOf<UUID, Int>()
        var iInstruction = 0
        program.transform { statementInstance ->
            val statement = statementInstance.create()
            when (statement) {
                is InstructionStatement -> {
                    yield(statementInstance)
                    ++iInstruction
                }
                is AnchorStatement -> {
                    anchorIndices[statement.anchor.uniqueId] = iInstruction
                    // The anchor statement is removed, as it is already resolved and therefore no longer required.
                }
                else -> {
                    throw SourceCompilationException(statementInstance.source, "Unresolved statement type ${statementInstance.prototype.id}")
                }
            }
        }.onFailure { return Result.failure(it) }
        return Result.success(anchorIndices)
    }

    private fun generateInstructions(program: StatementProgram, architecture: Architecture<*>): Result<List<Instruction>> {
        val instructions = mutableListOf<Instruction>()
        for (statementInstance in program.statements) {
            val statement = statementInstance.create()
            if (statement !is InstructionStatement) {
                throw SourceCompilationException(statementInstance.source, "Non instruction statement ${statementInstance.prototype.id} found")
            }

            val output = if (statement is InstructionStatement.WithOutput) {
                parseInstructionRegister(
                    statement.output,
                    statementInstance.sourceOfOrDefault(InstructionStatement.WithOutput::output),
                    architecture,
                ).getOrElse { return Result.failure(it) }
            } else {
                architecture.registers().first()
            }

            val conditionRegister = parseInstructionRegister(
                statement.conditionValue,
                statementInstance.sourceOfOrDefault(InstructionStatement::conditionValue),
                architecture,
            ).getOrElse { return Result.failure(it) }
            val inputA = parseInstructionValue(
                statement.inputA,
                statementInstance.sourceOfOrDefault(InstructionStatement::inputA),
                architecture,
            ).getOrElse { return Result.failure(it) }
            val inputB = parseInstructionValue(
                statement.inputB,
                statementInstance.sourceOfOrDefault(InstructionStatement::inputB),
                architecture,
            ).getOrElse { return Result.failure(it) }

            instructions += Instruction(
                statement.operation,
                statement.condition,
                conditionRegister,
                output,
                inputA,
                inputB,
            )
        }
        return Result.success(instructions)
    }

    private fun parseInstructionRegister(value: ValueLike, source: SourceLink, architecture: Architecture<*>): Result<Register> {
        return when (value) {
            is RegisterLike.RegisterReference -> {
                Result.success(value.register)
            }
            is RegisterLike.PC -> {
                Result.success(architecture.programCounter)
            }
            else -> {
                Result.failure(SourceCompilationException(source, "Invalid register: $value"))
            }
        }
    }

    private fun parseInstructionValue(value: ValueLike, source: SourceLink, architecture: Architecture<*>): Result<ValueProvider> {
        return when (value) {
            is IntegerValue -> {
                Result.success(ImmediateValue(value.value))
            }
            is RegisterLike.RegisterReference -> {
                Result.success(value.register)
            }
            is RegisterLike.PC -> {
                Result.success(architecture.programCounter)
            }
            else -> {
                Result.failure(SourceCompilationException(source, "Invalid value: $value"))
            }
        }
    }

    public companion object {
        public const val AXIOM_ASM_EXTENSION: String = "axm"

        public const val START_LABEL_NAME: String = "global:start"
    }
}
