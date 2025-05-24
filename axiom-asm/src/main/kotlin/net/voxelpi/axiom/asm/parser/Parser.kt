package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.lexer.TokenizedStatement
import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.StatementInstance

public class Parser(
    public val transformations: List<ParserTransformation>,
) {

    public fun parse(statement: TokenizedStatement, scope: Scope): Result<StatementInstance> {
        for (rule in transformations) {
            if (!rule.isApplicable(statement)) {
                continue
            }

            val statement = rule.apply(statement, scope)
            return statement
        }

        return Result.failure(ParseException(statement.source, "No applicable parsing rule found."))
    }

    public companion object {

        public fun create(block: Builder.() -> Unit): Parser {
            val builder = Builder()
            builder.block()
            return Parser(builder.rules())
        }
    }

    public class Builder internal constructor() {

        private val transformations: MutableList<ParserTransformation> = mutableListOf()

        public fun rules(): List<ParserTransformation> {
            return transformations
        }

        public fun transformation(id: String, statement: Statement, block: ParserTransformation.Builder.() -> Unit): ParserTransformation {
            val rule = ParserTransformation.create(id, statement, block)
            transformations += rule
            return rule
        }
    }
}
