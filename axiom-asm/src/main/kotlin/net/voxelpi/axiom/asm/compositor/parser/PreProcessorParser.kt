package net.voxelpi.axiom.asm.compositor.parser

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.parser.TokenReader

internal object PreProcessorParser {

    public fun parse(tokens: List<LexerToken>): List<CompositorToken> {
        val preprocessedTokens = mutableListOf<CompositorToken>()
        val reader = TokenReader(tokens)

        val directive = reader.readTypedToken<LexerToken.Directive>()?.let { directive ->
            when (directive.value) {
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
                    throw SourcedCompilationException(directive.source, "Unknown directive ${directive.value}")
                }
            }
        }

        TODO()
    }
}
