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
import net.voxelpi.axiom.asm.pipeline.step.ReplaceStringConstantsStep
import net.voxelpi.axiom.asm.pipeline.step.ReplaceVariableNamesStep
import net.voxelpi.axiom.asm.pipeline.step.ResolveIfBlockStep
import net.voxelpi.axiom.asm.pipeline.step.ResolveScopeJumpStep
import net.voxelpi.axiom.asm.pipeline.step.ResolveVariableValuesStep
import net.voxelpi.axiom.asm.pipeline.step.UpcastLoadsStep
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.program.StatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.ConstantStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.statement.types.ProgramElementStatement
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ValueLike
import net.voxelpi.axiom.instruction.Instruction
import net.voxelpi.axiom.instruction.InstructionValue
import net.voxelpi.axiom.instruction.Program
import net.voxelpi.axiom.instruction.ProgramConstant
import net.voxelpi.axiom.instruction.ProgramElement
import net.voxelpi.axiom.register.RegisterVariable
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isRegularFile

public class Assembler(
    public val includeDirectories: List<Path>,
) {

    public fun assemble(
        text: String,
        architecture: Architecture,
        parser: Parser = Parsers.AXIOM_ASM,
        offset: Int = 0,
    ): Result<Program> {
        val compilationUnit = CompilationUnit("__main__", text)
        return assemble(compilationUnit, architecture, parser, offset)
    }

    public fun assemble(
        path: Path,
        architecture: Architecture,
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
        architecture: Architecture,
        parser: Parser = Parsers.AXIOM_ASM,
        position: Int = 0,
    ): Result<Program> = runCatching {
        val unitCollector = CompilationUnitCollector.create(unit, parser, includeDirectories, position).getOrThrow()
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

        // Replace special registers with references to actual registers.
        ReplaceSpecialRegistersStep(architecture).transform(program).getOrThrow()

        // Replace string constants with a list of characters.
        ReplaceStringConstantsStep.transform(program).getOrThrow()

        // Flatten the program.
        val (statementIndices, anchorIndices) = flattenProgram(program, architecture).getOrThrow()

        // Replace all anchor reference parameters with actual instruction indices.
        ApplyAnchorIndicesStep(anchorIndices).transform(program).getOrThrow()

        // Platform-dependent compatibility transformations.
        UpcastLoadsStep(architecture).transform(program).getOrThrow()

        val instructions = generateInstructions(program, architecture, statementIndices).getOrThrow()
        val compiledProgram = Program(instructions)
        return Result.success(compiledProgram)
    }

    private fun flattenProgram(
        program: MutableStatementProgram,
        architecture: Architecture,
    ): Result<Pair<List<Int>, Map<UUID, Int>>> {
        val anchorIndices = mutableMapOf<UUID, Int>()
        val statementsIndices = mutableListOf<Int>()
        val usedIndices = mutableMapOf<Int, SourceLink>()

        val activeIndexedScopes: ArrayDeque<Scope> = ArrayDeque(listOf(program.globalScope))
        val indexStack: ArrayDeque<Int> = ArrayDeque()
        var index = program.globalScope.position

        program.transform { statementInstance ->
            val statement = statementInstance.create()

            // Find common parent scope.
            val previousIndexScope = activeIndexedScopes.last()
            val scope = statementInstance.scope
            val commonScope = Scope.lastCommonScope(scope, previousIndexScope)
                ?: throw SourceCompilationException(statementInstance.source, "Something went SERIOUSLY wrong, this should never happen 💀.")

            // Handle closed scopes.
            var tempScope = previousIndexScope
            while (tempScope.uniqueId != commonScope.uniqueId) {
                if (tempScope.uniqueId == activeIndexedScopes.last().uniqueId) {
                    activeIndexedScopes.removeLast()
                    index = indexStack.removeLast()
                }
                tempScope = (tempScope as LocalScope).parent
            }

            // Handle opened scopes.
            if (commonScope.uniqueId != scope.uniqueId) {
                val commonScopeAncestry = commonScope.ancestry()
                val scopeAncestry = scope.ancestry()
                for (openedScope in scopeAncestry.subList(commonScopeAncestry.size, scopeAncestry.size)) {
                    val position = openedScope.position ?: continue
                    indexStack.addLast(index)
                    activeIndexedScopes.add(openedScope)
                    index = position
                }
            }

            // Handle statement.
            when (statement) {
                is ProgramElementStatement -> {
                    if (index in usedIndices) {
                        val firstDefinitionSource = usedIndices[index]!!
                        throw SourceCompilationException(
                            statementInstance.source,
                            "Program Memory overlap, the program memory address $index is already in use.",
                            SourceCompilationException(
                                firstDefinitionSource,
                                "Originally defined here."
                            )
                        )
                    }
                    if (index.toULong() >= architecture.programSize) {
                        throw SourceCompilationException(statementInstance.source, "Out of program memory (${architecture.programSize} words)")
                    }

                    yield(statementInstance)
                    statementsIndices += index
                    usedIndices[index] = statementInstance.source
                    index += 1
                }
                is AnchorStatement -> {
                    anchorIndices[statement.anchor.uniqueId] = index
                }
                else -> {
                    throw SourceCompilationException(statementInstance.source, "Unresolved statement type ${statementInstance.prototype.id}")
                }
            }
        }.onFailure { return Result.failure(it) }

        return Result.success(Pair(statementsIndices, anchorIndices))
    }

    private fun generateInstructions(
        program: StatementProgram,
        architecture: Architecture,
        statementPositions: List<Int>,
    ): Result<List<ProgramElement>> {
        // Create program memory full of nop instructions.
        val programElements = MutableList<ProgramElement>(architecture.programSize.toInt()) {
            ProgramConstant(0UL, emptyMap())
        }

        for ((index, statementInstance) in program.statements.withIndex()) {
            val statement = statementInstance.create()
            if (statement !is ProgramElementStatement) {
                throw SourceCompilationException(statementInstance.source, "Non instruction statement ${statementInstance.prototype.id} found")
            }
            val position = statementPositions[index]

            when (statement) {
                is ConstantStatement.IntegerConstant -> {
                    val value = statement.value
                    if (value !is IntegerValue) {
                        throw SourceCompilationException(statementInstance.source, "Program constant $index is not an integer value")
                    }

                    programElements[position] = ProgramConstant(
                        architecture.instructionWordType.unsignedValueOf(value.value.toULong()),
                        mapOf(SOURCE_INSTRUCTION_META_KEY to statementInstance.source),
                    )
                }
                is InstructionStatement -> {
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

                    programElements[position] = Instruction(
                        statement.operation,
                        statement.condition,
                        conditionRegister,
                        output,
                        inputA,
                        inputB,
                        mapOf(SOURCE_INSTRUCTION_META_KEY to statementInstance.source),
                    )
                }
            }
        }
        return Result.success(programElements)
    }

    private fun parseConditionRegister(value: RegisterLike, source: SourceLink, architecture: Architecture): Result<RegisterVariable> {
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

    private fun parseOutputRegister(value: RegisterLike, source: SourceLink, architecture: Architecture): Result<RegisterVariable> {
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

    private fun parseInstructionValue(value: ValueLike, source: SourceLink, architecture: Architecture): Result<InstructionValue> {
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

    private fun findRegister(architecture: Architecture, specification: RegisterLike.AnyRegister): Result<RegisterVariable> {
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

        public const val SOURCE_INSTRUCTION_META_KEY: String = "source"
    }
}
