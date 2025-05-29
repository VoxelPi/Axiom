package net.voxelpi.axiom.asm.lexer

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.util.parseInteger

/**
 * The lexer converts the string content of a compilation unit into a series of tokens.
 */
public class Lexer() {

    /**
     * Convert tokens
     */
    public fun tokenize(unit: CompilationUnit): List<TokenizedStatement> {
        val statements: MutableList<List<Token>> = mutableListOf()

        val text = unit.content
        val lines = text.split("\n")

        var iStartIndex = 0
        for ((lineNumber, rawLineText) in lines.withIndex()) {
            val lineText = if (rawLineText.endsWith('\r')) {
                rawLineText.substring(0, rawLineText.length - 1)
            } else {
                rawLineText
            }

            statements += tokenizeLine(lineText, iStartIndex, lineNumber, unit)
            iStartIndex += rawLineText.length + 1 // +1 for the new line character.
        }

        // Remove all empty statements.
        statements.removeAll { it.isEmpty() }

        // Return the list of all generated tokens.
        return statements.map { TokenizedStatement(it) }
    }

    private fun tokenizeLine(text: String, lineStartIndex: Int, lineNumber: Int, unit: CompilationUnit): List<List<Token>> {
        val statements: MutableList<List<Token>> = mutableListOf()

        val statementTexts = text.split(";")
        var iStartColumn = 0
        for (statementText in statementTexts) {
            val iStartIndex = lineStartIndex + iStartColumn
            statements += tokenizeStatement(statementText, iStartIndex, lineNumber, iStartColumn, unit)
            iStartColumn += statementText.length + 1

            // Check for comments in the statement.
            if ('#' in statementText) {
                // Ignore all remaining statements after a comment.
                break
            }
        }

        return statements
    }

    private fun tokenizeStatement(text: String, statementIndex: Int, lineNumber: Int, columnNumber: Int, unit: CompilationUnit): List<Token> {
        val statement: MutableList<Token> = mutableListOf()

        var iSymbol = 0
        symbolLoop@ while (iSymbol < text.length) {
            // Get the current character.
            val c = text[iSymbol]

            // Handle whitespace.
            if (c.isWhitespace()) {
                ++iSymbol
                continue
            }

            // Handle comments.
            if (c == '#') {
                // Comments end the statement.
                break
            }

            // Handle symbols.
            var isSymbol = false
            for (symbol in SYMBOLS) {
                if (text.startsWith(symbol, iSymbol)) {
                    // Ignore unary operators if the next character is a digit.
                    if ((symbol == "+" || symbol == "-") && iSymbol < text.length - 1 && text[iSymbol + 1].isDigit()) {
                        continue
                    }
                    // Ignore slash in text.
                    if (symbol == "/" && ((iSymbol < text.length - 1 && !text[iSymbol + 1].isWhitespace()) || (iSymbol > 0 && !text[iSymbol - 1].isWhitespace()))) {
                        continue
                    }

                    statement.add(Token.Text(symbol, SourceLink.CompilationUnitSlice(unit, statementIndex + iSymbol, lineNumber, columnNumber + iSymbol, symbol.length)))
                    iSymbol += symbol.length
                    continue@symbolLoop
                }
            }

            // Word tokens.
            // Store the current symbol index and increment until a whitespace or semicolon is encountered.
            val iTokenStart = iSymbol
            while (iSymbol < text.length) {
                val isSymbol = SYMBOLS.any {
                    if ((it == "+" || it == "-") && iSymbol < text.length - 1 && text[iSymbol + 1].isDigit()) {
                        return@any false
                    }
                    if (it == "/" && ((iSymbol < text.length - 1 && !text[iSymbol + 1].isWhitespace()) || (iSymbol > 0 && !text[iSymbol - 1].isWhitespace()))) {
                        return@any false
                    }
                    text.startsWith(it, iSymbol)
                }
                if (text[iSymbol].isWhitespace() || isSymbol) {
                    break
                }
                ++iSymbol
            }
            val iTokenEnd = iSymbol
            val tokenText = text.substring(iTokenStart, iTokenEnd)
            val wordSource = SourceLink.CompilationUnitSlice(unit, statementIndex + iTokenStart, lineNumber, columnNumber + iTokenStart, iTokenEnd - iTokenStart)

            // Labels
            if ("^@(?:[A-Za-z0-9_-]+:)?[A-Za-z0-9_\\-/]+".toRegex().matches(tokenText)) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Label(text.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Variables
            if ("^\\$(?:[A-Za-z0-9_-]+:)?[A-Za-z0-9_\\-/]+".toRegex().matches(tokenText)) {
                val iVariableNameStart = iTokenStart + 1
                statement.add(Token.Variable(text.substring(iVariableNameStart, iTokenEnd), wordSource))
                continue
            }

            // Integers.
            val parsedInteger = parseInteger(tokenText)
            if (parsedInteger != null) {
                statement.add(Token.Integer(parsedInteger, wordSource))
                continue
            }

            // Normal text.
            statement.add(Token.Text(tokenText, wordSource))
        }

        // Add the statement.
        return statement
    }

    public companion object {
        private val SYMBOLS = listOf(
            ":=",
            "+=",
            "-=",
            "*=",
            "/=",
            "%=",
            ">=",
            "<=",
            "==",
            "!=",
            "!",
            "+",
            "-",
            "*",
            "/",
            "%",
            "=",
            "[",
            "]",
            "(",
            ")",
            "{",
            "}",
            "<",
            ">",
            ",",
        )
    }
}
