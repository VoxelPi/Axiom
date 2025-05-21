package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.ScopeStatement
import net.voxelpi.axiom.asm.statement.Statement
import net.voxelpi.axiom.asm.statement.TokenizedStatement
import net.voxelpi.axiom.asm.statement.preprocessor.IncludeStatement

public class Parser {

    // private val rules: MutableMap<String, ParserRule> = mutableMapOf()
    private val rules: MutableList<ParserRule> = mutableListOf()

    init {
        rules += ParserRule.create("include/unit") {
            directive("include")
            val unit = textArgument("unit")

            then {
                IncludeStatement.Unit(statementSource(), valueOf(unit))
            }
        }

        rules += ParserRule.create("include/scope") {
            directive("include")
            val scope = labelArgument("scope")
            literal("from")
            val unit = textArgument("unit")

            then {
                IncludeStatement.Scope(statementSource(), valueOf(unit), valueOf(scope))
            }
        }

        rules += ParserRule.create("include/scope_with_alias") {
            directive("include")
            val scope = labelArgument("scope")
            literal("from")
            val unit = textArgument("unit")
            literal("as")
            val alias = labelArgument("alias")

            then {
                IncludeStatement.ScopeWithAlias(statementSource(), valueOf(unit), valueOf(scope), valueOf(alias))
            }
        }

        rules += ParserRule.create("scope_open_unnamed") {
            then {
                ScopeStatement.OpenScope.Unnamed(statementSource())
            }
        }

        rules += ParserRule.create("scope_open_named") {
            val name = labelArgument("name")
            curlyBracketsOpen()

            then {
                ScopeStatement.OpenScope.Named(statementSource(), valueOf(name))
            }
        }

        rules += ParserRule.create("scope_close") {
            curlyBracketsClose()

            then {
                ScopeStatement.CloseScope(statementSource())
            }
        }
    }

    public fun parse(statement: TokenizedStatement): Result<Statement> {
        TODO()
    }
}
