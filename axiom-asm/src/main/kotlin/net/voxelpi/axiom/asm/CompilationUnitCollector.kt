package net.voxelpi.axiom.asm

import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.statement.IncludeStatement
import net.voxelpi.axiom.asm.statement.MutableStatementSequence
import java.nio.file.Path

internal class CompilationUnitCollector(
    val mainUnit: CompilationUnit,
    val parser: Parser,
    val includeDirectories: Collection<Path>,
) {
    val lexer: Lexer = Lexer()

    val globalScope: GlobalScope = GlobalScope()

    val mainProgram: MutableStatementSequence = prepareUnit(mainUnit).getOrThrow()

    private val units: MutableMap<String, CompilationUnit> = mutableMapOf(mainUnit.id to mainUnit)
    private val unitStatements: MutableMap<String, MutableStatementSequence> = mutableMapOf(mainUnit.id to mainProgram)

    fun collect(): Result<MutableStatementSequence> {
        val uncheckedUnits = ArrayDeque(listOf(mainUnit))
        while (uncheckedUnits.isNotEmpty()) {
            val unit = uncheckedUnits.removeFirst()
        }

        return Result.success(mainProgram)
    }

    private fun prepareUnit(unit: CompilationUnit): Result<MutableStatementSequence> {
        // Tokenize input text.
        val lexer = Lexer()
        val tokenizedStatements = lexer.tokenize(unit)

        val globalScope = GlobalScope()

        // Parse tokenized statements
        val statements = MutableStatementSequence(
            globalScope,
            tokenizedStatements.map {
                parser.parse(it, globalScope).getOrElse { exception -> return Result.failure(exception) }
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
