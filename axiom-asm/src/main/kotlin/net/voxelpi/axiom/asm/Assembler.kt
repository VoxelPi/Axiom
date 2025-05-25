package net.voxelpi.axiom.asm

import net.voxelpi.axiom.ImmediateValue
import net.voxelpi.axiom.Register
import net.voxelpi.axiom.ValueProvider
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.parser.Parsers
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.statement.types.LabelStatement
import net.voxelpi.axiom.asm.statement.types.VariableStatement
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

    public fun assemble(unit: CompilationUnit, architecture: Architecture<*>, parser: Parser = Parsers.AXIOM_ASM): Result<Program> {
        val unitCollector = CompilationUnitCollector.create(unit, parser, includeDirectories).getOrElse {
            return Result.failure(it)
        }
        val program = unitCollector.reduce().getOrElse {
            return Result.failure(it)
        }

        program.transformType<VariableStatement.Definition> { statementInstance ->
            val statement = statementInstance.create()
            statementInstance.scope.defineVariable(statement.name.name, statement.value).getOrElse {
                throw SourceCompilationException(statementInstance.source, it.message ?: "")
            }
        }.getOrElse { return Result.failure(it) }
        program.transformType<LabelStatement.Definition> { statementInstance ->
            val statement = statementInstance.create()
            val label = statementInstance.scope.defineLabel(statement.name.name).getOrElse {
                throw SourceCompilationException(statementInstance.source, it.message ?: "")
            }
            program.anchors[label.uniqueId] = label

            // Create the corresponding anchor statement.
            yield(ANCHOR_STATEMENT_PROTOTYPE.createInstance(AnchorStatement(label), statementInstance.scope, statementInstance.source))
        }.getOrElse { return Result.failure(it) }

        // Define the start label if it is not yet defined
        if (!program.globalScope.isLabelDefined(START_LABEL_NAME)) {
            val label = program.globalScope.defineLabel(START_LABEL_NAME).getOrElse {
                return Result.failure(CompilationException("Unable to define implicit start label. ${it.message}"))
            }
            program.anchors[label.uniqueId] = label

            // Create the corresponding anchor statement.
            program.statements.add(0, ANCHOR_STATEMENT_PROTOTYPE.createInstance(AnchorStatement(label), program.globalScope, SourceLink.Generated("@start", "implicit start label")))
        }

        // Calculate anchors indices.
        val anchorIndices = mutableMapOf<UUID, Int>()
        var iStatement = 0
        for (statementInstance in program.statements) {
            val statement = statementInstance.create()
            when (statement) {
                is InstructionStatement -> {
                    ++iStatement
                }
                is AnchorStatement -> {
                    anchorIndices[statement.anchor.uniqueId] = iStatement
                }
                else -> {
                    throw SourceCompilationException(statementInstance.source, "Unresolved statement type ${statementInstance.prototype.id}")
                }
            }
        }

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
                ).getOrElse {  return Result.failure(it) }
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

        val compiledProgram = Program(instructions)
        return Result.success(compiledProgram)
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

        private val ANCHOR_STATEMENT_PROTOTYPE = StatementPrototype.fromType<AnchorStatement>().getOrThrow()
    }
}
