package net.voxelpi.axiom.asm.frontend.lexer.transform

import net.voxelpi.axiom.asm.exception.SourcedCompilationException
import net.voxelpi.axiom.asm.frontend.lexer.Token
import net.voxelpi.axiom.asm.language.BracketType
import net.voxelpi.axiom.asm.source.SourceReference

internal object BracketTokenTransformation : TokenTransformation {

    override fun transform(tokens: List<Token>): List<Token> {
        val outputTokens = mutableListOf<Token>()
        val scopeStarts: ArrayDeque<Pair<BracketType, SourceReference.UnitSlice>> = ArrayDeque()
        val scopes: ArrayDeque<MutableList<Token>> = ArrayDeque(listOf(outputTokens))

        var iToken = 0
        while (iToken < tokens.size) {
            val currentScope = scopes.last()

            val token = tokens[iToken]
            iToken += 1

            // If the token is not a symbol, it can simply put into the current scope.
            if (token !is Token.Symbol) {
                currentScope.add(token)
                continue
            }

            // Check for opening bracket
            BracketType.entries.find { it.openingSymbol == token.symbol }?.let { bracketType ->
                scopeStarts.addLast(Pair(bracketType, token.source))
                scopes.add(mutableListOf())
                continue
            }

            // Check for closing bracket
            BracketType.entries.find { it.closingSymbol == token.symbol }?.let { bracketType ->
                // Check if there is currently an open bracket scope.
                val (openingBracketType, openingBracketSource) = scopeStarts.removeLastOrNull()
                    ?: throw SourcedCompilationException(token.source, "Unmatched closing bracket")

                // Check if the opening and closing brackets are of the same type.
                if (openingBracketType != bracketType) {
                    throw SourcedCompilationException(token.source, "Mismatched closing bracket, \"${openingBracketType.openingSymbol}\" and \"${bracketType.closingSymbol}\"")
                }

                // Create the bracket token.
                val bracketScopeTokens = scopes.removeLast()
                val bracketToken = Token.Bracket(
                    bracketType,
                    bracketScopeTokens,
                    openingBracketSource,
                    token.source,
                )

                val newCurrentScope = scopes.last()
                newCurrentScope.add(bracketToken)
                continue
            }

            // Normal text token, add to the current scope token list.
            currentScope.add(token)
        }

        // Check if any scopes remain open.
        scopeStarts.firstOrNull()?.let {
            val (openingBracketType, openingBracketSource) = it
            throw SourcedCompilationException(openingBracketSource, "Unmatched opening bracket \"${openingBracketType.openingSymbol}\"")
        }

        return outputTokens
    }
}
