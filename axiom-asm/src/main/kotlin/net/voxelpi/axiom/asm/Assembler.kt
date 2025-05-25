package net.voxelpi.axiom.asm

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.parser.Parser
import net.voxelpi.axiom.asm.parser.Parsers
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

        val compilationUnit = CompilationUnit(path.normalize().absolutePathString(), path.toFile().readText())
        return assemble(compilationUnit)
    }

    public fun assemble(unit: CompilationUnit, parser: Parser = Parsers.AXIOM_ASM): Result<Program> {
        val unitCollector = CompilationUnitCollector.create(unit, parser, includeDirectories).getOrElse {
            return Result.failure(it)
        }
        val program = unitCollector.reduce().getOrElse {
            return Result.failure(it)
        }

        return Result.failure(CompilationException("Not yet implemented (${program.statements.size})"))
    }

    public companion object {
        public const val AXIOM_ASM_EXTENSION: String = "axm"
    }
}
