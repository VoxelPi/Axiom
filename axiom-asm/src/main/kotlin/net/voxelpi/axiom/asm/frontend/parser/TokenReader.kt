package net.voxelpi.axiom.asm.frontend.parser

import net.voxelpi.axiom.asm.frontend.lexer.LexerToken
import net.voxelpi.axiom.asm.frontend.parser.value.ValueParser
import net.voxelpi.axiom.asm.language.SeparatorType
import net.voxelpi.axiom.asm.source.SourcedValue
import net.voxelpi.axiom.asm.source.join

internal class TokenReader(
    private val tokens: List<LexerToken>,
    iStart: Int = 0,
) {

    private val snapshots: ArrayDeque<Int> = ArrayDeque()

    private var index: Int = iStart

    fun snapshot() {
        snapshots.addLast(index)
    }

    fun revert(): List<LexerToken> {
        val iEnd = index
        index = snapshots.removeLast()
        return tokens.subList(index, iEnd)
    }

    fun accept(): List<LexerToken> {
        val startIndex = snapshots.removeLast()
        return tokens.subList(startIndex, index)
    }

    fun remaining(): Int {
        if (index >= tokens.size) {
            return 0
        }
        return tokens.size - index
    }

    val head: LexerToken?
        get() {
            if (index >= tokens.size) {
                return null
            }
            return tokens[index]
        }

    fun readToken(): LexerToken? {
        if (index >= tokens.size) {
            return null
        }
        return tokens[index++]
    }

    fun readTokenIf(predicate: (token: LexerToken) -> Boolean): LexerToken? {
        if (index >= tokens.size) {
            return null
        }

        val token = tokens[index]
        if (!predicate(token)) {
            return null
        }

        index += 1
        return token
    }

    inline fun <reified T : LexerToken> readTypedToken(): T? {
        return readTokenIf { it is T } as T?
    }

    inline fun <reified T : LexerToken> readTypedTokenIf(noinline predicate: (token: T) -> Boolean): T? {
        return readTokenIf { it is T && predicate(it) } as T?
    }

    fun readSymbol(symbol: String): Boolean {
        return readTypedTokenIf<LexerToken.Symbol> { it.symbol == symbol } != null
    }

    fun readAnySeparator(): Int {
        return readSeparator(0..3)!!
    }

    fun readSeparator(level: Int): Int? {
        return readSeparator(level..level)
    }

    fun readSeparator(levels: IntRange): Int? {
        // EOF is treated as newline.
        if (index >= tokens.size) {
            return if (2 in levels) 2 else null
        }
        val token = tokens[index]

        // If the following token is not a separator, treat it as a level 0 separator.
        if (token !is LexerToken.Separator) {
            return if (0 in levels) 0 else null
        }
        val tokenLevel = token.type

        // Read separator token.
        val level = when (tokenLevel) {
            SeparatorType.STRONG -> 3
            SeparatorType.NORMAL -> 2
            SeparatorType.WEAK -> 1
        }
        if (level in levels) {
            index++
            return level
        }
        return null
    }

    /**
     * Returns a list of all tokens up to the first separator of the given [level].
     * The separator is also consumed but not put in the returned list
     */
    fun readUntilSeparator(level: SeparatorType): List<LexerToken> {
        val selected = mutableListOf<LexerToken>()
        while (remaining() > 0) {
            val token = readToken() ?: break
            if (token is LexerToken.Separator && token.type >= level) {
                break
            }
            selected += token
        }
        return selected
    }

    fun <T : Any> readValue(parser: ValueParser<T>): Result<T> {
        snapshot()
        val value = parser.parse(this).getOrElse {
            revert()
            return Result.failure(it)
        }
        accept()
        return Result.success(value)
    }

    fun <T : Any> readSourcedValue(parser: ValueParser<T>): Result<SourcedValue<T>> {
        snapshot()
        val value = parser.parse(this).getOrElse {
            revert()
            return Result.failure(it)
        }
        val tokens = accept()
        val source = tokens.map { it.source }.join()
        return Result.success(SourcedValue(value, parser.type, source))
    }
}
