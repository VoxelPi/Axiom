package net.voxelpi.axiom.asm

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.pipeline.step.BuildScopesStep
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.program.MutableStatementProgram
import net.voxelpi.axiom.asm.statement.program.StatementProgram
import net.voxelpi.axiom.asm.statement.types.AnchorStatement
import net.voxelpi.axiom.asm.statement.types.IncludeStatement
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.asm.type.UnitLike
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

internal class CompilationUnitCollector private constructor(
    val mainUnit: CompilationUnit,
    val mainProgram: MutableStatementProgram,
    val parser: Parser,
    val includeDirectories: Collection<Path>,
) {
    val lexer: Lexer = Lexer()

    val globalScope: GlobalScope = GlobalScope()

    private val units: MutableMap<String, CompilationUnit> = mutableMapOf(mainUnit.id to mainUnit)
    private val unitsStatements: MutableMap<String, MutableStatementProgram> = mutableMapOf(mainUnit.id to mainProgram)

    fun reduce(): Result<MutableStatementProgram> {
        collect(mainProgram).onFailure {
            return Result.failure(it)
        }

        return reduceProgram(mainProgram, setOf(mainUnit.id))
    }

    private fun reduceProgram(inputProgram: StatementProgram, unitTrace: Set<String>): Result<MutableStatementProgram> {
        val program: MutableStatementProgram = inputProgram.mutableCopy()

        var iStatement = 0
        while (iStatement < program.statements.size) {
            val statementInstance = program.statements[iStatement]
            val statement = statementInstance.create()
            if (statement !is IncludeStatement) {
                ++iStatement
                continue
            }

            // Delete original include statement.
            program.statements.removeAt(iStatement)

            val unitParameter = statement.unit
            when (unitParameter) {
                is UnitLike.UnitName -> throw SourceCompilationException(statementInstance.source, "Encountered unresolved unit reference \"${unitParameter.name}\" in include statement.")
                is UnitLike.UnitReference -> {}
            }
            val unit = unitParameter.unit
            if (unit.id in unitTrace) {
                return Result.failure(SourceCompilationException(statementInstance.source, "Cyclic unit inclusion."))
            }
            val unitProgram = reduceProgram(unitsStatements[unit.id]!!, unitTrace + unit.id).getOrElse {
                return Result.failure(it)
            }

            when (statement) {
                is IncludeStatement.Unit -> {
                    // Copy over all relevant scopes.
                    val sortedIncludedScopes = unitProgram.sortedScopes()
                    val includedScopeMapping = mutableMapOf<UUID, Scope>()
                    for (includedScope in sortedIncludedScopes) {
                        when (includedScope) {
                            is GlobalScope -> {
                                includedScopeMapping[includedScope.uniqueId] = statementInstance.scope

                                // Copy over all labels.
                                for (label in includedScope.labels.values) {
                                    statementInstance.scope.labels[label.name] = label
                                }
                                // Copy over all variables (should be not necessary).
                                for (variable in includedScope.variables.values) {
                                    statementInstance.scope.variables[variable.name] = variable
                                }
                            }
                            is LocalScope.Named -> {
                                val parentScope = includedScopeMapping[includedScope.parent.uniqueId]!!
                                val scope = parentScope.createScope(
                                    includedScope.name,
                                    includedScope.uniqueId,
                                    includedScope.variables.toMutableMap(),
                                    includedScope.labels.toMutableMap(),
                                    includedScope.startAnchor.uniqueId,
                                    includedScope.endAnchor.uniqueId,
                                )
                                program.scopes[scope.uniqueId] = scope
                                includedScopeMapping[includedScope.uniqueId] = scope
                            }
                            is LocalScope.Unnamed -> {
                                val parentScope = includedScopeMapping[includedScope.parent.uniqueId]!!
                                val scope = parentScope.createScope(
                                    includedScope.uniqueId,
                                    includedScope.variables.toMutableMap(),
                                    includedScope.labels.toMutableMap(),
                                    includedScope.startAnchor.uniqueId,
                                    includedScope.endAnchor.uniqueId,
                                )
                                program.scopes[scope.uniqueId] = scope
                                includedScopeMapping[includedScope.uniqueId] = scope
                            }
                        }
                    }

                    // Copy over all anchors.
                    for (includedAnchor in unitProgram.anchors.values) {
                        when (includedAnchor) {
                            is ScopeAnchor.ScopeStart -> {
                                val mappedScope = includedScopeMapping[includedAnchor.scope.uniqueId]!! as LocalScope // A local scope can't become a global scope.
                                program.anchors[includedAnchor.uniqueId] = ScopeAnchor.ScopeStart(includedAnchor.uniqueId, mappedScope)
                            }
                            is ScopeAnchor.ScopeEnd -> {
                                val mappedScope = includedScopeMapping[includedAnchor.scope.uniqueId]!! as LocalScope // A local scope can't become a global scope.
                                program.anchors[includedAnchor.uniqueId] = ScopeAnchor.ScopeEnd(includedAnchor.uniqueId, mappedScope)
                            }
                            else -> {
                                program.anchors[includedAnchor.uniqueId] = includedAnchor
                            }
                        }
                    }

                    // Copy over all statements.
                    val includedStatements = unitProgram.statements.map { includedStatementInstance ->
                        val scope = includedScopeMapping[includedStatementInstance.scope.uniqueId]!!

                        val parameterValues = includedStatementInstance.parameterValues.mapValues { (_, value) ->
                            when (value) {
                                is Anchor -> {
                                    program.anchors[value.uniqueId]!!
                                }
                                is Scope -> {
                                    program.scopes[value.uniqueId]!!
                                }
                                is ScopeLike.ScopeReference -> {
                                    ScopeLike.ScopeReference(program.scopes[value.scope.uniqueId]!!)
                                }
                                else -> {
                                    value
                                }
                            }
                        }

                        StatementInstance(
                            includedStatementInstance.prototype,
                            scope,
                            includedStatementInstance.source,
                            parameterValues,
                            includedStatementInstance.parameterSources,
                        )
                    }
                    program.statements.addAll(iStatement, includedStatements)

                    // Skip over all included statements, as they have already been processed.
                    iStatement += includedStatements.size
                    continue
                }
                is IncludeStatement.Scope -> {
                    val scopeName = statement.scope.name

                    // Copy over all relevant scopes.
                    val sortedIncludedScopes = unitProgram.sortedScopes()
                    val includedScopeMapping = mutableMapOf<UUID, Scope>()
                    var includedScopeStartAnchor: ScopeAnchor.ScopeStart? = null
                    var includedScopeEndAnchor: ScopeAnchor.ScopeEnd? = null
                    for (includedScope in sortedIncludedScopes) {
                        when (includedScope) {
                            is GlobalScope -> {} // Do nothing, as global scopes can't be included by scope.
                            is LocalScope.Named -> {
                                if (includedScope.parent is GlobalScope && includedScope.name == scopeName) {
                                    val name = when (statement) {
                                        is IncludeStatement.Scope.Direct -> includedScope.name
                                        is IncludeStatement.Scope.WithAlias -> statement.alias.name
                                    }

                                    // The include scope itself.
                                    val parentScope = statementInstance.scope
                                    val scope = parentScope.createScope(
                                        name,
                                        includedScope.uniqueId,
                                        includedScope.variables.toMutableMap(),
                                        includedScope.labels.toMutableMap(),
                                        includedScope.startAnchor.uniqueId,
                                        includedScope.endAnchor.uniqueId,
                                    )
                                    includedScopeStartAnchor = scope.startAnchor
                                    includedScopeEndAnchor = scope.endAnchor
                                    program.scopes[scope.uniqueId] = scope
                                    includedScopeMapping[includedScope.uniqueId] = scope
                                    includedScopeMapping[includedScope.parent.uniqueId] = parentScope
                                } else {
                                    if (includedScope.parent.uniqueId in includedScopeMapping) {
                                        val parentScope = includedScopeMapping[includedScope.parent.uniqueId]!!
                                        val scope = parentScope.createScope(
                                            includedScope.name,
                                            includedScope.uniqueId,
                                            includedScope.variables.toMutableMap(),
                                            includedScope.labels.toMutableMap(),
                                            includedScope.startAnchor.uniqueId,
                                            includedScope.endAnchor.uniqueId,
                                        )
                                        program.scopes[scope.uniqueId] = scope
                                        includedScopeMapping[includedScope.uniqueId] = scope
                                    }
                                }
                            }
                            is LocalScope.Unnamed -> {
                                if (includedScope.parent.uniqueId in includedScopeMapping) {
                                    val parentScope = includedScopeMapping[includedScope.parent.uniqueId]!!
                                    val scope = parentScope.createScope(
                                        includedScope.uniqueId,
                                        includedScope.variables.toMutableMap(),
                                        includedScope.labels.toMutableMap(),
                                        includedScope.startAnchor.uniqueId,
                                        includedScope.endAnchor.uniqueId,
                                    )
                                    program.scopes[scope.uniqueId] = scope
                                    includedScopeMapping[includedScope.uniqueId] = scope
                                }
                            }
                        }
                    }

                    if (includedScopeStartAnchor == null) {
                        return Result.failure(SourceCompilationException(statementInstance.source, "The starting anchor of the scope \"$scopeName\" was not found in the included unit."))
                    }
                    if (includedScopeEndAnchor == null) {
                        return Result.failure(SourceCompilationException(statementInstance.source, "The ending anchor of the scope \"$scopeName\" was not found in the included unit."))
                    }

                    val firstIncludedStatementIndex = unitProgram.statements.indexOfFirst {
                        if (it.prototype.type == AnchorStatement::class) {
                            val statement = it.create() as AnchorStatement
                            val anchor = statement.anchor
                            anchor is ScopeAnchor.ScopeStart && anchor.uniqueId == includedScopeStartAnchor.uniqueId
                        } else {
                            false
                        }
                    }
                    val lastIncludedStatementIndex = unitProgram.statements.indexOfFirst {
                        if (it.prototype.type == AnchorStatement::class) {
                            val statement = it.create() as AnchorStatement
                            val anchor = statement.anchor
                            anchor is ScopeAnchor.ScopeEnd && anchor.uniqueId == includedScopeEndAnchor.uniqueId
                        } else {
                            false
                        }
                    }

                    // Copy over all statements.
                    val includedStatements = unitProgram.statements
                        .subList(firstIncludedStatementIndex, lastIncludedStatementIndex + 1)
                        .map { includedStatementInstance ->
                            val scope = includedScopeMapping[includedStatementInstance.scope.uniqueId]!!

                            val parameterValues = includedStatementInstance.parameterValues.mapValues { (_, value) ->
                                when (value) {
                                    is Anchor -> {
                                        // Copy over anchor if not already present.
                                        if (value.uniqueId !in program.anchors) {
                                            val includedAnchor = value
                                            when (includedAnchor) {
                                                is ScopeAnchor.ScopeStart -> {
                                                    val mappedScope = includedScopeMapping[includedAnchor.scope.uniqueId]!! as LocalScope // A local scope can't become a global scope.
                                                    program.anchors[includedAnchor.uniqueId] = ScopeAnchor.ScopeStart(includedAnchor.uniqueId, mappedScope)
                                                }
                                                is ScopeAnchor.ScopeEnd -> {
                                                    val mappedScope = includedScopeMapping[includedAnchor.scope.uniqueId]!! as LocalScope // A local scope can't become a global scope.
                                                    program.anchors[includedAnchor.uniqueId] = ScopeAnchor.ScopeEnd(includedAnchor.uniqueId, mappedScope)
                                                }
                                                else -> {
                                                    program.anchors[includedAnchor.uniqueId] = includedAnchor
                                                }
                                            }
                                        }

                                        // Swap reference for copied anchor.
                                        program.anchors[value.uniqueId]!!
                                    }
                                    is Scope -> {
                                        program.scopes[value.uniqueId]!!
                                    }
                                    is ScopeLike.ScopeReference -> {
                                        ScopeLike.ScopeReference(program.scopes[value.scope.uniqueId]!!)
                                    }
                                    else -> {
                                        value
                                    }
                                }
                            }

                            StatementInstance(
                                includedStatementInstance.prototype,
                                scope,
                                includedStatementInstance.source,
                                parameterValues,
                                includedStatementInstance.parameterSources,
                            )
                        }
                    program.statements.addAll(iStatement, includedStatements)

                    // Skip over all included statements, as they have already been processed.
                    iStatement += includedStatements.size
                    continue
                }
            }
        }

        return Result.success(program)
    }

    private fun collect(program: MutableStatementProgram): Result<Unit> {
        program.transformType<IncludeStatement> { statementInstance ->
            val statement = statementInstance.create()

            val unitNameArgument = statement.unit
            if (unitNameArgument !is UnitLike.UnitName) {
                throw SourceCompilationException(statementInstance.sourceOfOrDefault(IncludeStatement::unit), "Only unit names are allowed in include statements.")
            }
            val unitName = unitNameArgument.name

            // Check if the unit is already resolved.
            val unit = if (unitName in units) {
                units[unitName]!!
            } else {
                // Resolve the unit.
                val unitPaths = includeDirectories.mapNotNull {
                    val file = it / "${unitName}.${Assembler.AXIOM_ASM_EXTENSION}"
                    if (file.exists() && file.isRegularFile()) {
                        file
                    } else {
                        null
                    }
                }
                if (unitPaths.isEmpty()) {
                    throw IllegalArgumentException("The unit $unitName was not found in any of the include directories.")
                }
                if (unitPaths.size > 1) {
                    throw IllegalArgumentException("The unit $unitName was found in multiple include <= directories.")
                }
                val unitPath = unitPaths.first()

                val unit = CompilationUnit(unitName, unitPath.toFile().readText())
                units[unit.id] = unit

                // Prepare the statements from the unit.
                val unitStatements = prepareUnit(unit, parser).getOrElse {
                    throw SourceCompilationException(statementInstance.source, "Failed to prepare the unit $unitName.", it)
                }
                unitsStatements[unit.id] = unitStatements

                // Collect the unit.
                collect(unitStatements).getOrThrow()

                // Return the unit
                unit
            }

            // Yield the include statement, with the unit name replaced with a unit reference.
            yield(
                statementInstance.modifiedCopy {
                    this[IncludeStatement::unit] = UnitLike.UnitReference(unit)
                }
            )
        }.onFailure {
            return Result.failure(it)
        }

        return Result.success(Unit)
    }

    companion object {

        private fun prepareUnit(unit: CompilationUnit, parser: Parser): Result<MutableStatementProgram> {
            // Tokenize input text.
            val lexer = Lexer()
            val tokenizedStatements = lexer.tokenize(unit)

            val unitGlobalScope = GlobalScope()

            // Parse tokenized statements
            val program = MutableStatementProgram(
                unitGlobalScope,
                tokenizedStatements.map {
                    parser.parse(it, unitGlobalScope).getOrElse { exception -> return Result.failure(exception) }
                },
            )

            // Build scopes.
            BuildScopesStep.transform(program).getOrElse { return Result.failure(it) }

            return Result.success(program)
        }

        fun create(
            mainUnit: CompilationUnit,
            parser: Parser,
            includeDirectories: Collection<Path>,
        ): Result<CompilationUnitCollector> {
            val mainProgram: MutableStatementProgram = prepareUnit(mainUnit, parser).getOrElse {
                return Result.failure(it)
            }
            return Result.success(CompilationUnitCollector(mainUnit, mainProgram, parser, includeDirectories))
        }
    }
}
