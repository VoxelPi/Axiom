package net.voxelpi.axiom.asm

import net.voxelpi.axiom.asm.lexer.Lexer
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.parser.Parsers
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.instruction.Program
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile

public class Assembler(
    public val includeDirectories: List<Path>,
) {

    public fun assemble(text: String): Result<Program> {
        val compilationUnit = CompilationUnit("__provided__", text)
        return assemble(compilationUnit)
    }

    public fun assemble(path: Path): Result<Program> {
        if (!path.isRegularFile()) {
            return Result.failure(IllegalArgumentException("The path $path is not a regular file."))
        }

        val compilationUnit = CompilationUnit(path.absolutePathString(), path.toFile().readText())
        return assemble(compilationUnit)
    }

    public fun assemble(unit: CompilationUnit, parser: Parser = Parsers.AXIOM_ASM): Result<Program> {
        // Tokenize input text.
        val lexer = Lexer()
        val tokenizedStatements = lexer.tokenize(unit)

        val globalScope = GlobalScope()

        // Parse tokenized statements
        val statements = tokenizedStatements.map {
            parser.parse(it, globalScope).getOrElse { exception -> return Result.failure(exception) }
        }.toMutableList()

        TODO()
    }
}
