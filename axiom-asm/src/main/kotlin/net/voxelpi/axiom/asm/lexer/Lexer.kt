package net.voxelpi.axiom.asm.lexer

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.exception.SourceCompilationException
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
        val statements: MutableList<TokenizedStatement> = mutableListOf()

        val text = unit.content
        val lines = text.split("\n")

        var iStartIndex = 0
        for ((lineNumber, rawLineText) in lines.withIndex()) {
            val lineText = rawLineText.trimEnd()

            statements += tokenizeLine(lineText, iStartIndex, lineNumber, unit)
            iStartIndex += rawLineText.length + 1 // +1 for the new line character.
        }

        // Remove all empty statements.
        statements.removeAll { it.tokens.isEmpty() }

        // Return the list of all generated token statements.
        return statements
    }

    private fun tokenizeLine(text: String, lineStartIndex: Int, lineNumber: Int, unit: CompilationUnit): List<TokenizedStatement> {
        val statements: MutableList<TokenizedStatement> = mutableListOf()

        val splitStatementTexts = text.split(";")

        // Rejoin statements with open string literals.
        val statementTexts = mutableListOf<String>()
        var accumulatedText = ""
        for (splitText in splitStatementTexts) {
            val text = accumulatedText + splitText
            var i = accumulatedText.length
            var bracketOpen = accumulatedText.isNotEmpty()

            var isEscaped = false
            while (i < text.length) {
                if (isEscaped) {
                    isEscaped = false
                    continue
                }
                if (text[i] == '\\') {
                    isEscaped = true
                    continue
                }
                if (text[i] == '"') {
                    bracketOpen = !bracketOpen
                }
                ++i
            }

            if (bracketOpen) {
                accumulatedText = "$text;"
            } else {
                statementTexts += text
            }
        }

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

    private fun tokenizeStatement(text: String, statementIndex: Int, lineNumber: Int, columnNumber: Int, unit: CompilationUnit): TokenizedStatement {
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

            if (c == '"') {
                val iStringStart = iSymbol // Index of opening string quote.
                iSymbol += 1
                var isEscaped = false
                var characters = mutableListOf<Char>()
                while (iSymbol < text.length) {
                    if (isEscaped) {
                        characters += text[iSymbol]
                        iSymbol += 1
                        isEscaped = false
                        continue
                    }
                    if (text[iSymbol] == '"') {
                        break
                    }
                    if (text[iSymbol] == '\\') {
                        isEscaped = true
                        continue
                    }
                    characters += text[iSymbol]
                    iSymbol += 1
                }

                val iStringEnd = iSymbol // Index of closing string quote.
                iSymbol += 1

                if (iStringEnd == text.length) {
                    val exceptionLink = SourceLink.CompilationUnitSlice(
                        unit,
                        statementIndex + iStringStart,
                        lineNumber,
                        columnNumber + iStringStart,
                        text.length - iStringStart,
                    )
                    throw SourceCompilationException(exceptionLink, "Missing closing quotes.")
                }

                val source = SourceLink.CompilationUnitSlice(
                    unit,
                    statementIndex + iStringStart,
                    lineNumber,
                    columnNumber + iStringStart,
                    iStringEnd - iStringStart + 1,
                )
                statement.add(Token.StringText(characters.joinToString(""), source))
                continue
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

            // Integers.
            val parsedInteger = parseInteger(tokenText)
            if (parsedInteger != null) {
                statement.add(Token.Integer(parsedInteger, wordSource))
                continue
            }

            // Labels
            if (tokenText.startsWith("@")) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Label(text.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Variables
            if (tokenText.startsWith("$")) {
                val iVariableNameStart = iTokenStart + 1
                statement.add(Token.Variable(text.substring(iVariableNameStart, iTokenEnd), wordSource))
                continue
            }

            // Normal text.
            statement.add(Token.Text(tokenText, wordSource))
        }

        // Add the statement.
        return TokenizedStatement(SourceLink.CompilationUnitSlice(unit, statementIndex, lineNumber, columnNumber, text.length), statement)
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
