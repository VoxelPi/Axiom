package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.frontend.lexer.transform.BracketTokenTransformation
import net.voxelpi.axiom.asm.frontend.lexer.transform.CharacterTokenTransformation
import net.voxelpi.axiom.asm.frontend.lexer.transform.IntegerTokenMapping
import net.voxelpi.axiom.asm.frontend.lexer.transform.StrongSeparatorTokenMapping
import net.voxelpi.axiom.asm.frontend.lexer.transform.TrimScopeTokenTransformation
import net.voxelpi.axiom.asm.source.SourceUnit

public object Lexer {

    public fun tokenize(source: SourceUnit): List<Token> {
        var tokens = Tokenizer.tokenize(source)

        // Apply all transformations.
        for (transformation in TOKEN_TRANSFORMATIONS) {
            tokens = transformation.transform(tokens)
        }

        return tokens
    }

    private val TOKEN_TRANSFORMATIONS = listOf(
        CharacterTokenTransformation,
        IntegerTokenMapping,
        StrongSeparatorTokenMapping,
        BracketTokenTransformation,
        TrimScopeTokenTransformation,
    )
}
