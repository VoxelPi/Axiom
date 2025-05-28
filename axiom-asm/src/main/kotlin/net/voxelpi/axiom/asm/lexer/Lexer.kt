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
        val statement: MutableList<Token> = mutableListOf()

        var line = 0
        var iLineStart = 0
        var iSymbol = 0
        while (iSymbol < unit.content.length) {
            // Get the current character.
            val c = unit.content[iSymbol]
            val cSource = SourceLink.CompilationUnitSlice(unit, iSymbol, line, (iSymbol - iLineStart), 1)

            // Handle comments.
            if (c == '#') {
                while (iSymbol < unit.content.length && !(unit.content[iSymbol] == ';' || unit.content[iSymbol] == '\n')) {
                    ++iSymbol
                }
                continue
            }

            // Handle single character tokens.
            when (c) {
                '\n' -> {
                    // End the current statement and begin a new line.
                    statements.add(statement.toList())
                    statement.clear()
                    ++line
                    ++iSymbol
                    iLineStart = iSymbol
                    continue
                }
                ';' -> {
                    // End the current statement.
                    statements.add(statement.toList())
                    statement.clear()
                    ++iSymbol
                    continue
                }
                '{' -> {
                    statement.add(Token.CurlyBrackets.Open(cSource))
                    ++iSymbol
                    continue
                }
                '}' -> {
                    statement.add(Token.CurlyBrackets.Close(cSource))
                    ++iSymbol
                    continue
                }
                '[' -> {
                    statement.add(Token.SquareBrackets.Open(cSource))
                    ++iSymbol
                    continue
                }
                ']' -> {
                    statement.add(Token.SquareBrackets.Close(cSource))
                    ++iSymbol
                    continue
                }
                ',' -> {
                    statement.add(Token.Text(",", cSource))
                    ++iSymbol
                    continue
                }
            }

            // Whitespace.
            if (c.isWhitespace()) {
                ++iSymbol
                continue
            }

            // Word tokens.
            // Store the current symbol index and increment until a whitespace or semicolon is encountered.
            val iTokenStart = iSymbol
            while (iSymbol < unit.content.length && !(unit.content[iSymbol].isWhitespace() || unit.content[iSymbol] == ';' || unit.content[iSymbol] == ',')) {
                ++iSymbol
            }
            val iTokenEnd = iSymbol
            val tokenText = unit.content.substring(iTokenStart, iTokenEnd)
            val wordSource = SourceLink.CompilationUnitSlice(unit, iTokenStart, line, (iTokenStart - iLineStart), iTokenEnd - iTokenStart)

            // Labels
            if ("^@(?:[A-Za-z0-9_-]+:)?[A-Za-z0-9_\\-/]+".toRegex().matches(tokenText)) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Label(unit.content.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Variables
            if ("^\\$(?:[A-Za-z0-9_-]+:)?[A-Za-z0-9_\\-/]+".toRegex().matches(tokenText)) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Variable(unit.content.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Pre-Processor directives
            if ("^!(\\w+)".toRegex().matches(tokenText)) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Directive(unit.content.substring(iLabelNameStart, iTokenEnd), wordSource))
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

        // Add the last statement.
        statements.add(statement.toList())

        // Remove all empty statements.
        statements.removeAll { it.isEmpty() }

        // Return the list of all generated tokens.
        return statements.map { TokenizedStatement(it) }
    }
}
