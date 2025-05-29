package net.voxelpi.axiom.asm.lexer

import net.voxelpi.axiom.asm.source.SourceLink

public sealed interface Token {

    /**
     * The source from the token.
     */
    public val source: SourceLink.CompilationUnitSlice

    /**
     * A text token.
     */
    public data class Text(
        val value: String,
        override val source: SourceLink.CompilationUnitSlice,
    ) : Token

    /**
     * An integer token.
     */
    public data class Integer(
        val value: Long,
        override val source: SourceLink.CompilationUnitSlice,
    ) : Token

    /**
     * A variable token.
     */
    public data class Variable(
        val value: String,
        override val source: SourceLink.CompilationUnitSlice,
    ) : Token

    /**
     * A label token.
     */
    public data class Label(
        val value: String,
        override val source: SourceLink.CompilationUnitSlice,
    ) : Token
}
