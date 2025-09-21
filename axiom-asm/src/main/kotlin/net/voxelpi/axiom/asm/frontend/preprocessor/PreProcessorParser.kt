package net.voxelpi.axiom.asm.frontend.preprocessor

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.parser.TokenReader

internal object PreProcessorParser {

    public fun parse(tokens: List<LexerToken>): List<PreProcessorToken> {
        val preprocessedTokens = mutableListOf<PreProcessorToken>()
        val reader = TokenReader(tokens)

        if (reader.readSymbol("!")) {
            val directive = reader.readTypedToken<LexerToken.Symbol>()
                ?: throw SourcedCompilationException(reader.head!!.source, "Expected a directive")

            when (directive.symbol) {
                "include" -> {
                    reader.readSeparator(level = 1)
                        ?: throw SourcedCompilationException(reader.head!!.source, "Missing included file path")

                    val path = reader.readTypedToken<LexerToken.StringLiteral>()
                        ?: throw SourcedCompilationException(reader.head!!.source, "Missing included file path")

                    // preprocessedTokens.add()
                    TODO()
                }
                "template" -> {
                    TODO()
                }
                "insert" -> {
                    TODO()
                }
                "at" -> {
                    TODO()
                }
                "public" -> {
                    TODO()
                }
                "global" -> {
                    TODO()
                }
                "define" -> {
                    TODO()
                }
                else -> {
                    throw SourcedCompilationException(directive.source, "Unknown directive ${directive.symbol}")
                }
            }
        }

        TODO()
    }
}
