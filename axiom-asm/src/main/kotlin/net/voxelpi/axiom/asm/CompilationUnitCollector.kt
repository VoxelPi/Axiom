package net.voxelpi.axiom.asm

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.anchor.ScopeAnchor
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.LocalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.sequence.MutableStatementSequence
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
    val mainProgram: MutableStatementSequence,
    val parser: Parser,
    val includeDirectories: Collection<Path>,
) {
    val lexer: Lexer = Lexer()

    val globalScope: GlobalScope = GlobalScope()

    private val units: MutableMap<String, CompilationUnit> = mutableMapOf(mainUnit.id to mainUnit)
    private val unitsStatements: MutableMap<String, MutableStatementSequence> = mutableMapOf(mainUnit.id to mainProgram)

    fun reduce(): Result<MutableStatementSequence> {
        val program: MutableStatementSequence = mainProgram.copy()

        collect(program).onFailure {
            return Result.failure(it)
        }

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

            val unit = statement.unit
            when (unit) {
                is UnitLike.UnitName -> throw SourceCompilationException(statementInstance.source, "Encountered unresolved unit reference \"${unit.name}\" in include statement.")
                is UnitLike.UnitReference -> {}
            }

            when (statement) {
                is IncludeStatement.Unit -> {
                    val unit = (statement.unit as UnitLike.UnitReference).unit
                    val unitProgram = unitsStatements[unit.id]!!

                    // Copy over all relevant scopes.
                    val sortedIncludedScopes = unitProgram.sortedScopes()
                    val includedScopeMapping = mutableMapOf<UUID, Scope>()
                    for (includedScope in sortedIncludedScopes) {
                        when (includedScope) {
                            is GlobalScope -> {
                                includedScopeMapping[includedScope.uniqueId] = statementInstance.scope
                            }
                            is LocalScope.Named -> {
                                val parentScope = includedScopeMapping[includedScope.parent.uniqueId]!!
                                val scope = LocalScope.Named(
                                    parentScope,
                                    includedScope.uniqueId,
                                    includedScope.name,
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
                                val scope = LocalScope.Unnamed(
                                    parentScope,
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
                        val scope = includedScopeMapping[statementInstance.scope.uniqueId]!!

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

                    // We have to go through the included program, as there could again be another include.
                    // Therefore, we DO NOT increase the statement index.
                    continue
                }
                is IncludeStatement.Scope.Direct -> {
                    TODO()
                }
                is IncludeStatement.Scope.WithAlias -> {
                    TODO()
                }
            }
        }

        // program.transformType<IncludeStatement> { statementInstance ->
        //     val includeStatement = statementInstance.create()
        //
        //     when (includeStatement) {
        //         is IncludeStatement.Unit -> {
        //             val unitIdValue = includeStatement.unit
        //             val unitIdSource = statementInstance.sourceOfOrDefault(IncludeStatement::unit)
        //             require(unitIdValue is UnitLike.UnitName) { "Only unit names are allowed in include statements." }
        //             val unitId = unitIdValue.name
        //             val unitStatements = unitsStatements[unitId] ?: throw SourceCompilationException(unitIdSource, "The unit $unitId was not found.")
        //
        //             val unitScopes = unitStatements.scopes.mapValues { (_, scope) ->
        //                 if (scope is GlobalScope) {
        //                     scopes
        //                 }
        //             }
        //
        //             for (unitStatement in unitStatements.statements) {
        //                 yield(
        //                     StatementInstance(
        //                         unitStatement.prototype,
        //                         unitStatement.scope,
        //                         unitStatement.source,
        //                         unitStatement.parameterValues,
        //                         unitStatement.parameterSources,
        //                     )
        //                 )
        //             }
        //         }
        //         is IncludeStatement.Scope.Direct -> {}
        //         is IncludeStatement.Scope.WithAlias -> {}
        //     }
        // }
        return Result.success(mainProgram)
    }

    private fun collect(program: MutableStatementSequence): Result<Unit> {
        program.transformType<IncludeStatement> { statementInstance ->
            val statement = statementInstance.create()

            val unitNameArgument = statement.unit
            require(unitNameArgument is UnitLike.UnitName) { "Only unit names are allowed in include statements." }
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
                collect(unitStatements)

                // Return the unit
                unit
            }

            // Yield the include statement, with the unit name replaced to a unit reference.
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

        private fun prepareUnit(unit: CompilationUnit, parser: Parser): Result<MutableStatementSequence> {
            // Tokenize input text.
            val lexer = Lexer()
            val tokenizedStatements = lexer.tokenize(unit)

            val unitGlobalScope = GlobalScope()

            // Parse tokenized statements
            val statements = MutableStatementSequence(
                unitGlobalScope,
                tokenizedStatements.map {
                    parser.parse(it, unitGlobalScope).getOrElse { exception -> return Result.failure(exception) }
                },
            )

            // Build scopes.
            statements.buildScopes()

            return Result.success(statements)
        }

        fun create(
            mainUnit: CompilationUnit,
            parser: Parser,
            includeDirectories: Collection<Path>,
        ): Result<CompilationUnitCollector> {
            val mainProgram: MutableStatementSequence = prepareUnit(mainUnit, parser).getOrElse {
                return Result.failure(it)
            }
            return Result.success(CompilationUnitCollector(mainUnit, mainProgram, parser, includeDirectories))
        }
    }
}
