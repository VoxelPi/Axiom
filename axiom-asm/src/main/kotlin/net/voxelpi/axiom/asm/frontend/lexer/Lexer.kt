package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.source.SourceUnit

public object Lexer {

    public fun tokenize(source: SourceUnit): List<Token> {
        val tokens = Tokenizer.tokenize(source)

        return tokens
    }
}
