package net.voxelpi.axiom.asm.frontend.lexer

import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.source.SourceReference
import net.voxelpi.axiom.asm.source.SourceUnit
import kotlin.streams.toList

internal object Tokenizer {

    fun tokenize(unit: SourceUnit): List<LexerToken> {
        // Early exit for completely empty program.
        if (unit.text.isBlank()) {
            return emptyList()
        }

        val tokens = mutableListOf<LexerToken>()

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
                tokens += LexerToken.Separator.Normal(whitespaceSource)
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
    ): List<LexerToken> {
        val tokens: MutableList<LexerToken> = mutableListOf()
        val codePoints = text.codePoints().toList()

        var iStartWhitespace: Int? = null

        var iCodePoint = 0
        while (iCodePoint < codePoints.size) {
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
                    tokens.add(LexerToken.Separator.Weak(whitespaceSource))
                    iStartWhitespace = null
                }
            }

            // Symbol token
            val symbolMatch = SYMBOLS.find(text.substring(iChar)::startsWith)
            if (symbolMatch != null) {
                val symbolSource = SourceReference.UnitSlice(
                    unit,
                    lineStartIndex + iChar,
                    1,
                )
                tokens.add(LexerToken.Symbol(symbolMatch, symbolSource))
                iCodePoint += symbolMatch.codePointCount(0, symbolMatch.length)
                continue
            }

            // Find text end.
            var iCodePointWordEnd = codePoints.size

            // Find next whitespace.
            for (i in (iCodePoint + 1)..<iCodePointWordEnd) {
                if (Character.isWhitespace(codePoints[i])) {
                    iCodePointWordEnd = i
                    break
                }
            }

            // Find next symbol.
            var iCharWordEnd = text.offsetByCodePoints(0, iCodePointWordEnd)
            for (symbol in SYMBOLS) {
                val iCharOccurrence = text.substring(iChar, iCharWordEnd).indexOf(symbol)
                if (iCharOccurrence != -1 && iCharOccurrence < iCharWordEnd) {
                    iCharWordEnd = iChar + iCharOccurrence
                }
            }
            // iCodePointWordEnd = iCodePoint + text.substring(iChar, iCharWordEnd).codePointCount(0, iCharWordEnd - iChar)

            // Generate text token.
            val wordText = text.substring(iChar..<iCharWordEnd)
            val tokenSource = SourceReference.UnitSlice(
                unit,
                lineStartIndex + iChar,
                wordText.length,
            )
            tokens.add(LexerToken.Text(wordText, tokenSource))
            iCodePoint += text.substring(iChar, iCharWordEnd).codePointCount(0, wordText.length)
        }

        return tokens
    }

    private val SYMBOLS = setOf(
        ";",
        ",",
        ".",
        "!",
        "?",
        "\"",
        "'",
        "\\",
        "=",
        "*",
        "+",
        "-",
        "/",
        "%",
        "$",
        "@",
        ":",
        "^",
        "|",
        "&",
        "<",
        ">",
        *BracketType.entries.map { it.openingSymbol }.toTypedArray(),
        *BracketType.entries.map { it.closingSymbol }.toTypedArray(),
    )
}
