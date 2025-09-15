package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.streams.toList

internal object Tokenizer {

    fun tokenize(unit: SourceUnit): List<Token> {
        // Early exit for completely empty program.
        if (unit.text.isBlank()) {
            return emptyList()
        }

        val tokens = mutableListOf<Token>()

        val text = unit.text
        val lines = text.split("\n")

        var iWhitespaceStart: Int? = null

        var iStartIndex = 0
        for (lineText in lines) {
            // Extract everything up to the first comment symbol.
            val nonCommentLength = lineText.indexOf('#').let { if (it == -1) lineText.length else it }
            val nonCommentText = lineText.take(nonCommentLength)

            // Skip the line if there is no actual code.
            if (nonCommentText.isBlank()) {
                iStartIndex += lineText.length + 1 // +1 for the new line character.
                continue
            }

            // Handle whitespace.
            val iFirstNoneWhitespace = nonCommentText.indexOfFirst { !it.isWhitespace() }
            val iLastNoneWhitespaceWhitespace = nonCommentText.indexOfLast { !it.isWhitespace() }
            check(iFirstNoneWhitespace != -1) { "Blank line detected, should be already handled" }
            check(iLastNoneWhitespaceWhitespace != -1) { "Blank line detected, should be already handled" }
            if (iWhitespaceStart != null) {
                val iWhitespaceEnd = iStartIndex + iFirstNoneWhitespace
                val whitespaceSource = SourceReference.UnitSlice(
                    unit,
                    iWhitespaceStart,
                    iWhitespaceEnd - iWhitespaceStart
                )
                tokens += Token.Separator.Normal(whitespaceSource)
            }
            iWhitespaceStart = iStartIndex + iLastNoneWhitespaceWhitespace + 1

            // Handle content.
            val contentText = lineText.substring(iFirstNoneWhitespace, iLastNoneWhitespaceWhitespace + 1)
            val iContentStartIndex = iStartIndex + iFirstNoneWhitespace
            tokens += tokenizeLine(contentText, iContentStartIndex, unit)

            // Go the next line.
            iStartIndex += lineText.length + 1 // +1 for the new line character.
        }

        // Return the list of all generated token statements.
        return tokens
    }

    private fun tokenizeLine(
        text: String,
        lineStartIndex: Int,
        unit: SourceUnit,
    ): List<Token> {
        val tokens: MutableList<Token> = mutableListOf()
        val codePoints = text.codePoints().toList()

        var iStartWhitespace: Int? = null

        var iCodePoint = 0
        symbol_loop@ while (iCodePoint < codePoints.size) {
            // Get the current character.
            // val c = text[iSymbol]
            val codePoint = codePoints[iCodePoint]
            val iChar = text.offsetByCodePoints(0, iCodePoint)
            // val codePoint = text.codePointAt(iSymbol)
            // val codePointWidth = text.offsetByCodePoints(iSymbol, 1)

            // Ignore whitespace.
            if (Character.isWhitespace(codePoint)) {
                if (iStartWhitespace == null) {
                    iStartWhitespace = iChar
                }
                ++iCodePoint
                continue
            } else {
                if (iStartWhitespace != null) {
                    val whitespaceSource = SourceReference.UnitSlice(
                        unit,
                        lineStartIndex + iStartWhitespace,
                        iChar - iStartWhitespace,
                    )
                    tokens.add(Token.Separator.Weak(whitespaceSource))
                    iStartWhitespace = null
                }
            }

            // Text token
            val wordMatch = WORD_PATTERN.find(text.substring(iChar))
            if (wordMatch != null) {
                val tokenSource = SourceReference.UnitSlice(
                    unit,
                    lineStartIndex + iChar,
                    wordMatch.value.length,
                )
                tokens.add(Token.Symbol(wordMatch.value, tokenSource))
                iCodePoint += wordMatch.value.codePointCount(0, wordMatch.value.length)
                continue
            }

            // Symbol token
            val symbolSource = SourceReference.UnitSlice(
                unit,
                lineStartIndex + iChar,
                1,
            )
            tokens.add(Token.Symbol(String(Character.toChars(codePoint)), symbolSource))
            iCodePoint += 1
        }

        return tokens
    }

    private val WORD_PATTERN = "^[\\w\\p{L}]+".toRegex()
}
