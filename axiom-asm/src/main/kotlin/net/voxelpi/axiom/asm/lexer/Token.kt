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
     * A register alias token.
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

    /**
     * A label token.
     */
    public data class Directive(
        val value: String,
        override val source: SourceLink.CompilationUnitSlice,
    ) : Token

    /**
     * A scope token.
     */
    public sealed interface CurlyBrackets : Token {
        public data class Open(
            override val source: SourceLink.CompilationUnitSlice,
        ) : CurlyBrackets

        public data class Close(
            override val source: SourceLink.CompilationUnitSlice,
        ) : CurlyBrackets
    }

    /**
     * A memory address token.
     */
    public sealed interface SquareBrackets : Token {
        public data class Open(
            override val source: SourceLink.CompilationUnitSlice,
        ) : SquareBrackets

        public data class Close(
            override val source: SourceLink.CompilationUnitSlice,
        ) : SquareBrackets
    }
}
