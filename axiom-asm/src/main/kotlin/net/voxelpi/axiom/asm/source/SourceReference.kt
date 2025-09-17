package net.voxelpi.axiom.asm.source

import net.voxelpi.axiom.asm.exception.CompilationException
import kotlin.ranges.coerceAtMost
import kotlin.text.count
import kotlin.text.indexOfLast
import kotlin.text.substring

/**
 * A reference to the source code that created a language element.
 */
public sealed interface SourceReference {

    /**
     * The text is the referenced source code.
     */
    public val text: String

    /**
     * A reference to a slice of a source unit.
     * @property unit the compilation unit.
     * @property index the index of the text in the unit.
     * @property line the line of the text in the unit.
     * @property column the column of the first character in the unit.
     * @property length the length of the text in the unit.
     */
    public data class UnitSlice(
        val unit: SourceUnit,
        val index: Int,
        val length: Int,
    ) : SourceReference {

        override val text: String
            get() = unit.text.substring(index, (index + length).coerceAtMost(unit.text.length))

        /**
         * The line of the referenced source.
         * The first row is row 1.
         */
        public val line: Int
            get() = unit.text.take(index).count { it == '\n' }

        /**
         * The column of the referenced source.
         * The first column is column 1.
         */
        public val column: Int
            get() {
                val lastNewLine = unit.text.take(index).indexOfLast { it == '\n' }
                return if (lastNewLine == -1) {
                    index
                } else {
                    index - lastNewLine
                }
            }

        override fun toString(): String {
            return "\"${text}\" at $line:$column in ${unit.id}"
        }
    }

    /**
     * A reference to generated text.
     * @property text the text of the source.
     * @property generator the generator of the source.
     */
    public data class Generated(
        override val text: String,
        val generator: String,
    ) : SourceReference
}

/**
 * Joins a list of unit slice references to a single unit slice reference.
 */
public fun List<SourceReference.UnitSlice>.join(): SourceReference.UnitSlice {
    if (isEmpty()) {
        throw CompilationException("Unable to join empty token list")
    }

    val iStart = minOf { it.index }
    val iEnd = maxOf { it.index + it.length }
    val length = iEnd - iStart
    return SourceReference.UnitSlice(first().unit, iStart, length)
}
