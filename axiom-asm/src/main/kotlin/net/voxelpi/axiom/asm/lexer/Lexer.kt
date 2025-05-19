package net.voxelpi.axiom.asm.lexer

import net.voxelpi.axiom.asm.CompilationUnit
import net.voxelpi.axiom.asm.source.SourceLink

/**
 * The lexer converts the string content of a compilation unit into a series of tokens.
 */
public class Lexer() {

    /**
     * Convert tokens
     */
    public fun tokenize(unit: CompilationUnit): List<List<Token>> {
        val statements: MutableList<List<Token>> = mutableListOf()
        val statement: MutableList<Token> = mutableListOf()

        var line = 0
        var iLineStart = 0
        var iSymbol = 0
        while (iSymbol < unit.content.length) {
            // Get the current character.
            val c = unit.content[iSymbol]
            val cSource = SourceLink.CompilationUnitSlice(unit, iSymbol, line, (iSymbol - iLineStart), 1)

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
                    statement.add(Token.Scope.Open(cSource))
                    ++iSymbol
                    continue
                }
                '}' -> {
                    statement.add(Token.Scope.Close(cSource))
                    ++iSymbol
                    continue
                }
                '[' -> {
                    statement.add(Token.MemoryAddress.Open(cSource))
                    ++iSymbol
                    continue
                }
                ']' -> {
                    statement.add(Token.MemoryAddress.Close(cSource))
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
            while (iSymbol < unit.content.length && !(unit.content[iSymbol].isWhitespace() || unit.content[iSymbol] == ';')) {
                ++iSymbol
            }
            val iTokenEnd = iSymbol
            val tokenText = unit.content.substring(iTokenStart, iTokenEnd)
            val wordSource = SourceLink.CompilationUnitSlice(unit, iTokenStart, line, (iTokenStart - iLineStart), iTokenEnd - iTokenStart)

            // Labels
            if (tokenText.startsWith("@")) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Label(unit.content.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Variables
            if (tokenText.startsWith("$")) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.Variable(unit.content.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Pre-Processor directives
            if (tokenText.startsWith("!")) {
                val iLabelNameStart = iTokenStart + 1
                statement.add(Token.PreProcessorDirective(unit.content.substring(iLabelNameStart, iTokenEnd), wordSource))
                continue
            }

            // Base-16 integer.
            if ("^[+-]?(0[xX][0-9a-fA-F_]+)$".toRegex().matches(tokenText)) {
                val (explicitSign, sign) = parseNumberSign(tokenText)
                val start = if (explicitSign) 3 else 2
                val integerText = tokenText.substring(start).replace("_", "")
                val integer = integerText.toLong(16) * sign
                statement.add(Token.Integer(integer, wordSource))
                continue
            }

            // Base-10 integer.
            if ("^[+-]?(0[dD]?[0-9_]+)$".toRegex().matches(tokenText)) {
                val (explicitSign, sign) = parseNumberSign(tokenText)
                val start = if (explicitSign) 3 else 2
                val integerText = tokenText.substring(start).replace("_", "")
                val integer = integerText.toLong(10) * sign
                statement.add(Token.Integer(integer, wordSource))
                continue
            }

            // Base-8 integer.
            if ("^[+-]?(0[oO][0-7_]+)$".toRegex().matches(tokenText)) {
                val (explicitSign, sign) = parseNumberSign(tokenText)
                val start = if (explicitSign) 3 else 2
                val integerText = tokenText.substring(start).replace("_", "")
                val integer = integerText.toLong(8) * sign
                statement.add(Token.Integer(integer, wordSource))
                continue
            }

            // Base-2 integer.
            if ("^[+-]?(0[bB][01_]+)$".toRegex().matches(tokenText)) {
                val (explicitSign, sign) = parseNumberSign(tokenText)
                val start = if (explicitSign) 3 else 2
                val integerText = tokenText.substring(start).replace("_", "")
                val integer = integerText.toLong(2) * sign
                statement.add(Token.Integer(integer, wordSource))
                continue
            }

            // Base-10 integer (implicit).
            if ("^[+-]?([0-9][0-9_]*)$".toRegex().matches(tokenText)) {
                val (explicitSign, sign) = parseNumberSign(tokenText)
                val start = if (explicitSign) 1 else 0
                val integerText = tokenText.substring(start).replace("_", "")
                val integer = integerText.toLong(10) * sign
                statement.add(Token.Integer(integer, wordSource))
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
        return statements
    }

    private fun parseNumberSign(text: String): Pair<Boolean, Int> {
        if (text.startsWith("-")) {
            return Pair(true, -1)
        }
        if (text.startsWith("+")) {
            return Pair(true, 1)
        }
        return Pair(false, 1)
    }
}
