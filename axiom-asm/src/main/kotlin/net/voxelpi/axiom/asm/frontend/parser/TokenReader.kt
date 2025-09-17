package net.voxelpi.axiom.asm.frontend.parser

import net.voxelpi.axiom.asm.frontend.lexer.Token
import net.voxelpi.axiom.asm.frontend.parser.value.ValueParser
import net.voxelpi.axiom.asm.source.SourcedValue
import net.voxelpi.axiom.asm.source.join

internal class TokenReader(
    private val tokens: List<Token>,
    iStart: Int = 0,
) {

    private val snapshots: ArrayDeque<Int> = ArrayDeque()

    private var index: Int = iStart

    fun snapshot() {
        snapshots.addLast(index)
    }

    fun revert(): List<Token> {
        val iEnd = index
        index = snapshots.removeLast()
        return tokens.subList(index, iEnd)
    }

    fun accept(): List<Token> {
        val startIndex = snapshots.removeLast()
        return tokens.subList(startIndex, index)
    }

    fun remaining(): Int {
        if (index >= tokens.size) {
            return 0
        }
        return tokens.size - index
    }

    fun readToken(): Token? {
        if (index >= tokens.size) {
            return null
        }
        return tokens[index++]
    }

    fun readTokenIf(predicate: (token: Token) -> Boolean): Token? {
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

    inline fun <reified T : Token> readTypedToken(): T? {
        return readTokenIf { it is T } as T?
    }

    inline fun <reified T : Token> readTypedTokenIf(noinline predicate: (token: T) -> Boolean): T? {
        return readTokenIf { it is T && predicate(it) } as T?
    }

    fun readSymbol(symbol: String): Boolean {
        return readTypedTokenIf<Token.Symbol> { it.symbol == symbol } != null
    }

    fun readAnySeparator(): Int {
        return readSeparator(0..3)!!
    }

    fun readSeparator(levels: IntRange): Int? {
        // EOF is treated as newline.
        if (index >= tokens.size) {
            return if (2 in levels) 2 else null
        }
        val token = tokens[index]

        // If the following token is not a separator, treat it as a level 0 separator.
        if (token !is Token.Separator) {
            return if (0 in levels) 0 else null
        }

        // Read separator token.
        val level = when (token) {
            is Token.Separator.Strong -> 3
            is Token.Separator.Normal -> 2
            is Token.Separator.Weak -> 1
        }
        if (level in levels) {
            index++
            return level
        }
        return null
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
