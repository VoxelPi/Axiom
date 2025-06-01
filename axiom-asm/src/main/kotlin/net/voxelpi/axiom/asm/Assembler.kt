package net.voxelpi.axiom.asm

import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.parser.Parsers
import net.voxelpi.axiom.asm.pipeline.step.ApplyAnchorIndicesStep
import net.voxelpi.axiom.asm.pipeline.step.DefineImplicitStartLabelStep
import net.voxelpi.axiom.asm.pipeline.step.DefineLabelsStep
import net.voxelpi.axiom.asm.pipeline.step.DefineVariablesStep
import net.voxelpi.axiom.asm.pipeline.step.InsertStartJumpStep
import net.voxelpi.axiom.asm.pipeline.step.ReplaceLabelNamesStep
import net.voxelpi.axiom.asm.pipeline.step.ReplaceRegisterNamesStep
import net.voxelpi.axiom.asm.pipeline.step.ReplaceScopeNamesStep
import net.voxelpi.axiom.asm.pipeline.step.ReplaceSpecialRegistersStep
import net.voxelpi.axiom.asm.pipeline.step.ReplaceVariableNamesStep
import net.voxelpi.axiom.asm.pipeline.step.ResolveIfBlockStep
import net.voxelpi.axiom.asm.pipeline.step.ResolveScopeJumpStep
import net.voxelpi.axiom.asm.pipeline.step.ResolveVariableValuesStep
import net.voxelpi.axiom.asm.pipeline.step.UpcastLoadsStep
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.program.StatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Operation
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.register.RegisterVariable
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isRegularFile

public class Assembler(
    public val includeDirectories: List<Path>,
) {

    public fun assemble(
        text: String,
        architecture: Architecture<*, *>,
        parser: Parser = Parsers.AXIOM_ASM,
        offset: Int = 0,
    ): Result<Program> {
        val compilationUnit = CompilationUnit("__main__", text)
        return assemble(compilationUnit, architecture, parser, offset)
    }

    public fun assemble(
        path: Path,
        architecture: Architecture<*, *>,
        parser: Parser = Parsers.AXIOM_ASM,
        offset: Int = 0,
    ): Result<Program> {
        if (!path.isRegularFile()) {
            return Result.failure(IllegalArgumentException("The path $path is not a regular file."))
        }

        val compilationUnit = CompilationUnit("__main__", path.toFile().readText())
        return assemble(compilationUnit, architecture, parser, offset)
    }

    public fun assemble(
        unit: CompilationUnit,
        architecture: Architecture<*, *>,
        parser: Parser = Parsers.AXIOM_ASM,
        offset: Int = 0,
    ): Result<Program> = runCatching {
        val unitCollector = CompilationUnitCollector.create(unit, parser, includeDirectories).getOrThrow()
        val program = unitCollector.reduce().getOrThrow()

        // Replace scope names with references.
        ReplaceScopeNamesStep.transform(program).getOrElse { return Result.failure(it) }

        // Replace register names with references to actual registers.
        ReplaceRegisterNamesStep(architecture).transform(program).getOrThrow()

        // Define variables.
        DefineVariablesStep.transform(program).getOrThrow()
        ReplaceVariableNamesStep.transform(program).getOrThrow()

        // Define labels.
        DefineLabelsStep.transform(program).getOrThrow()
        DefineImplicitStartLabelStep.transform(program).getOrThrow()
        ReplaceLabelNamesStep.transform(program).getOrThrow()

        // Transform if blocks to jump instructions.
        ResolveIfBlockStep.transform(program).getOrThrow()

        // Transform scope jumps to jump instructions.
        ResolveScopeJumpStep.transform(program).getOrThrow()

        // Resolve variable values.
        ResolveVariableValuesStep.transform(program).getOrThrow()

        // Insert start jump if neccessary.
        InsertStartJumpStep(architecture).transform(program).getOrThrow()

        // Generate anchors indices and use them to replace all anchor reference parameters.
        val anchorIndices = generateAnchorIndices(program, offset).getOrThrow()
        ApplyAnchorIndicesStep(anchorIndices).transform(program).getOrThrow()

        // Replace special registers with references to actual registers.
        ReplaceSpecialRegistersStep(architecture).transform(program).getOrThrow()

        // Platform-dependent compatibility transformations.
        UpcastLoadsStep(architecture).transform(program).getOrThrow()

        val instructions = generateInstructions(program, architecture, offset).getOrThrow()
        val compiledProgram = Program(instructions)
        return Result.success(compiledProgram)
    }

    private fun generateAnchorIndices(program: MutableStatementProgram, offset: Int): Result<Map<UUID, Int>> {
        val anchorIndices = mutableMapOf<UUID, Int>()
        var iInstruction = offset
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

    private fun generateInstructions(program: StatementProgram, architecture: Architecture<*, *>, offset: Int): Result<List<Instruction>> {
        val instructions = mutableListOf<Instruction>()
        val nopInstruction = Instruction(
            Operation.LOAD,
            Condition.NEVER,
            findRegister(architecture, RegisterLike.AnyRegister(conditionable = true)).getOrElse { return Result.failure(it) },
            findRegister(architecture, RegisterLike.AnyRegister(writable = true)).getOrElse { return Result.failure(it) },
            InstructionValue.ImmediateValue(0),
            InstructionValue.ImmediateValue(0),
        )
        repeat(offset) {
            instructions += nopInstruction
        }
        for (statementInstance in program.statements) {
            val statement = statementInstance.create()
            if (statement !is InstructionStatement) {
                throw SourceCompilationException(statementInstance.source, "Non instruction statement ${statementInstance.prototype.id} found")
            }

            val output = if (statement is InstructionStatement.WithOutput) {
                parseOutputRegister(
                    statement.output,
                    statementInstance.sourceOfOrDefault(InstructionStatement.WithOutput::output),
                    architecture,
                ).getOrElse { return Result.failure(it) }
            } else {
                findRegister(architecture, RegisterLike.AnyRegister(writable = true)).getOrElse { return Result.failure(it) }
            }

            val conditionRegister = parseConditionRegister(
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

    private fun parseConditionRegister(value: RegisterLike, source: SourceLink, architecture: Architecture<*, *>): Result<RegisterVariable<*, *>> {
        return when (value) {
            is RegisterLike.RegisterReference -> {
                Result.success(value.register)
            }
            is RegisterLike.AnyRegister -> {
                findRegister(architecture, value)
            }
            else -> {
                Result.failure(SourceCompilationException(source, "Invalid condition register: $value"))
            }
        }
    }

    private fun parseOutputRegister(value: RegisterLike, source: SourceLink, architecture: Architecture<*, *>): Result<RegisterVariable<*, *>> {
        return when (value) {
            is RegisterLike.RegisterReference -> {
                Result.success(value.register)
            }
            is RegisterLike.AnyRegister -> {
                findRegister(architecture, value)
            }
            else -> {
                Result.failure(SourceCompilationException(source, "Invalid output register: $value"))
            }
        }
    }

    private fun parseInstructionValue(value: ValueLike, source: SourceLink, architecture: Architecture<*, *>): Result<InstructionValue> {
        return when (value) {
            is IntegerValue -> {
                Result.success(InstructionValue.ImmediateValue(value.value))
            }
            is RegisterLike.RegisterReference -> {
                Result.success(InstructionValue.RegisterReference(value.register))
            }
            is RegisterLike.AnyRegister -> {
                val register = findRegister(architecture, value).getOrElse { return Result.failure(it) }
                Result.success(InstructionValue.RegisterReference(register))
            }
            else -> {
                Result.failure(SourceCompilationException(source, "Invalid value: $value"))
            }
        }
    }

    private fun findRegister(architecture: Architecture<*, *>, specification: RegisterLike.AnyRegister): Result<RegisterVariable<*, *>> {
        val register = architecture.registers.variables.values.firstOrNull {
            if (it.register.id == architecture.registers.programCounter.id) {
                return@firstOrNull false
            }
            if (specification.readable && !it.readable) {
                return@firstOrNull false
            }
            if (specification.writable && !it.writable) {
                return@firstOrNull false
            }
            if (specification.conditionable && !it.conditionable) {
                return@firstOrNull false
            }
            true
        }
        if (register == null) {
            return Result.failure(CompilationException("No register found with $specification."))
        }
        return Result.success(register)
    }

    public companion object {
        public const val AXIOM_ASM_EXTENSION: String = "axm"

        public const val START_LABEL_NAME: String = "global:start"
    }
}
