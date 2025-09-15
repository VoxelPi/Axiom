package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.frontend.lexer.transform.CharacterTokenTransformation
import net.voxelpi.axiom.asm.frontend.lexer.transform.IntegerTokenMapping
import net.voxelpi.axiom.asm.source.SourceUnit

public object Lexer {

    public fun tokenize(source: SourceUnit): List<Token> {
        var tokens = Tokenizer.tokenize(source)

        tokens = CharacterTokenTransformation.transform(tokens)
        tokens = IntegerTokenMapping.transform(tokens)

        return tokens
    }
}
