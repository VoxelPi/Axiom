package net.voxelpi.axiom.asm.frontend.parser.value

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.parser.TokenReader
import net.voxelpi.axiom.asm.language.NamespacedId
import kotlin.reflect.typeOf

internal object NamespacedIdParser : ValueParser<NamespacedId>(typeOf<NamespacedId>()) {

    override fun parse(tokens: TokenReader): Result<NamespacedId> {
        val token = tokens.readTypedToken<LexerToken.Text>()
            ?: return Result.failure(CompilationException("Expected namespace token"))

        val namespacedId = NamespacedId(token.value.split("::"))

        return Result.success(namespacedId)
    }
}
