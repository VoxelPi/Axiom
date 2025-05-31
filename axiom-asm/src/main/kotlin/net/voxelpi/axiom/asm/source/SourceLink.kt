package net.voxelpi.axiom.asm.source

import net.voxelpi.axiom.asm.CompilationUnit

public sealed interface SourceLink {

    /**
     * The text that is responsible for this element.
     */
    public val text: String

    /**
     * A unit source.
     * @property unit the compilation unit.
     * @property index the index of the text in the unit.
     * @property line the line of the text in the unit.
     * @property column the column of the first character in the unit.
     * @property length the length of the text in the unit.
     */
    public data class CompilationUnitSlice(
        val unit: CompilationUnit,
        val index: Int,
        val line: Int,
        val column: Int,
        val length: Int,
    ) : SourceLink {

        override val text: String
            get() = unit.content.substring(index, (index + length).coerceAtMost(unit.content.length))
    }

    /**
     * A generated source.
     * @property generator the generator of the source.
     * @property text the text of the source.
     */
    public data class Generated(
        override val text: String,
        val generator: String,
    ) : SourceLink
}
