package net.voxelpi.axiom.asm.frontend.parser.value

import net.voxelpi.axiom.asm.exception.CompilationException
import net.voxelpi.axiom.asm.frontend.lexer.Token
import net.voxelpi.axiom.asm.frontend.parser.TokenReader
import net.voxelpi.axiom.asm.language.NamespacedId
import kotlin.reflect.typeOf

internal object NamespacedIdParser : ValueParser<NamespacedId>(typeOf<NamespacedId>()) {

    override fun parse(tokens: TokenReader): Result<NamespacedId> {
        val firstToken = tokens.readTypedToken<Token.Symbol>()
            ?: return Result.failure(CompilationException("NamespacedId part has to be a valid text"))

        val parts = mutableListOf(firstToken.symbol)

        while (tokens.remaining() >= 3) {
            tokens.snapshot()
            if (!tokens.readSymbol(":")) {
                tokens.revert()
                break
            }
            if (!tokens.readSymbol(":")) {
                tokens.revert()
                break
            }
            val name = tokens.readTypedToken<Token.Symbol>()
            if (name == null) {
                tokens.revert()
                break
            }
            parts.add(name.symbol)
            tokens.accept()
        }

        val namespace = NamespacedId(parts)
        return Result.success(namespace)
    }
}
