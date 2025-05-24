package net.voxelpi.axiom.asm

import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.statement.MutableStatementSequence
import net.voxelpi.axiom.asm.statement.StatementPrototype
import net.voxelpi.axiom.asm.type.UnitLike
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

internal class CompilationUnitCollector(
    val mainUnit: CompilationUnit,
    val parser: Parser,
    val includeDirectories: Collection<Path>,
) {
    val lexer: Lexer = Lexer()

    val globalScope: GlobalScope = GlobalScope()

    val mainProgram: MutableStatementSequence = prepareUnit(mainUnit).getOrThrow()

    private val units: MutableMap<String, CompilationUnit> = mutableMapOf(mainUnit.id to mainUnit)
    private val unitsStatements: MutableMap<String, MutableStatementSequence> = mutableMapOf(mainUnit.id to mainProgram)

    init {
        collect(mainProgram)
    }

    fun reduce(): Result<MutableStatementSequence> {
        collect(mainProgram.copy()).getOrElse {
            return Result.failure(it)
        }

        val program: MutableStatementSequence = mainProgram.copy()

        program.transformType<IncludeStatement> { includeStatement ->
            when (includeStatement) {
                is IncludeStatement.Unit -> {
                    val unitIdArgument = includeStatement.unit
                    val unitIdValue = unitIdArgument.value
                    require(unitIdValue is UnitLike.UnitName) { "Only unit names are allowed in include statements." }
                    val unitId = unitIdValue.name
                    val unitStatements = unitsStatements[unitId] ?: throw ParseException(unitIdArgument.source, "The unit $unitId was not found.")

                    val unitScopes = unitStatements.scopes.mapValues { (_, scope) ->
                        if (scope is GlobalScope) {
                            // includeStatement.
                        }
                    }

                    for (unitStatement in unitStatements.statements) {
                        if (unitStatement !is StatementPrototype<*>) {
                            yield(unitStatement)
                            continue
                        }

                        yield(
                            StatementPrototype(
                                unitStatement.source,
                                unitStatement.type,
                                unitStatement.scope,
                                unitStatement.arguments,
                            )
                        )
                    }
                }
                is IncludeStatement.Scope.Direct -> {}
                is IncludeStatement.Scope.WithAlias -> {}
            }
        }
        return Result.success(mainProgram)
    }

    private fun collect(program: MutableStatementSequence): Result<Unit> {
        for (statement in program.statements) {
            if (statement !is IncludeStatement) {
                continue
            }

            val unitNameArgument = statement.unit.value
            require(unitNameArgument is UnitLike.UnitName) { "Only unit names are allowed in include statements." }
            val unitName = unitNameArgument.name

            // Check if the unit is already resolved.
            if (unitName in units) {
                continue
            }

            // Resolve the unit.
            val unitPaths = includeDirectories.mapNotNull {
                val file = it / "${unitName}.asm"
                if (file.exists() && file.isRegularFile()) {
                    file
                } else {
                    null
                }
            }
            if (unitPaths.isEmpty()) {
                return Result.failure(IllegalArgumentException("The unit $unitName was not found in any of the include directories."))
            }
            if (unitPaths.size > 1) {
                return Result.failure(IllegalArgumentException("The unit $unitName was found in multiple include directories."))
            }
            val unitPath = unitPaths.first()

            val unit = CompilationUnit(unitName, unitPath.toFile().readText())
            units[unit.id] = unit

            // Prepare the statements from the unit.
            val unitStatements = prepareUnit(unit).getOrElse {
                return Result.failure(it)
            }
            unitsStatements[unit.id] = unitStatements

            // Collect the unit.
            collect(unitStatements)
        }

        return Result.success(Unit)
    }

    private fun prepareUnit(unit: CompilationUnit): Result<MutableStatementSequence> {
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

        // Build include statements.
        statements.buildPrototypesWithType<IncludeStatement>().getOrElse {
            return Result.failure(it)
        }

        return Result.success(statements)
    }
}
