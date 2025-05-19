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
    public data class PreProcessorDirective(
        val value: String,
        override val source: SourceLink.CompilationUnitSlice,
    ) : Token

    /**
     * A scope token.
     */
    public sealed interface Scope : Token {
        public data class Open(
            override val source: SourceLink.CompilationUnitSlice,
        ) : Scope

        public data class Close(
            override val source: SourceLink.CompilationUnitSlice,
        ) : Scope
    }

    /**
     * A memory address token.
     */
    public sealed interface MemoryAddress : Token {
        public data class Open(
            override val source: SourceLink.CompilationUnitSlice,
        ) : MemoryAddress

        public data class Close(
            override val source: SourceLink.CompilationUnitSlice,
        ) : MemoryAddress
    }
}
