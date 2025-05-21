package net.voxelpi.axiom.asm.scope

import net.voxelpi.axiom.asm.lexer.Token
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement

public class ScopeCreationStep {

    public fun transform(statements: List<Statement>): List<Statement> {
        return sequence<Statement> {
            var iStatement = 0
            while (iStatement < statements.size) {
                val statement = statements[iStatement]
                require(statement is TokenizedStatement) { "Expected tokenized statement." }

                if (statement.tokens[0] is Token.CurlyBrackets.Open) {

                }

                ++iStatement
            }
        }.toList()
    }
}
