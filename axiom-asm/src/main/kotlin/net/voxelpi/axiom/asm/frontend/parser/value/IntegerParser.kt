package net.voxelpi.axiom.asm.frontend.parser.value

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.frontend.lexer.Token
import net.voxelpi.axiom.asm.frontend.parser.TokenReader
import kotlin.reflect.typeOf

internal object IntegerParser : ValueParser<Long>(typeOf<Long>()) {

    override fun parse(tokens: TokenReader): Result<Long> {
        val token = tokens.readTypedToken<Token.Integer>()
            ?: return Result.failure(CompilationException("Expected integer token"))

        return Result.success(token.value)
    }
}
