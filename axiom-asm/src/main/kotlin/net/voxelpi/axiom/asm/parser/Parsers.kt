package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.ScopeStatement
import net.voxelpi.axiom.asm.statement.instruction.ArithmeticStatements
import net.voxelpi.axiom.asm.statement.preprocessor.IncludeStatement

public object Parsers {

    public val AXIOM_ASM: Parser = Parser.create {
        rule<IncludeStatement.Unit>("include/unit") {
            directive("include")
            textArgument("unit")
        }

        rule<IncludeStatement.Scope>("include/scope") {
            directive("include")
            labelArgument("scope")
            literal("from")
            textArgument("unit")
        }

        rule<IncludeStatement.ScopeWithAlias>("include/scope_with_alias") {
            directive("include")
            labelArgument("scope")
            literal("from")
            textArgument("unit")
            literal("as")
            labelArgument("alias")
        }

        rule<ScopeStatement.OpenScope.Unnamed>("scope_open_unnamed") {
            curlyBracketsOpen()
        }

        rule<ScopeStatement.OpenScope.Named>("scope_open_named") {
            labelArgument("name")
            curlyBracketsOpen()
        }

        rule<ScopeStatement.CloseScope>("scope_close") {
            curlyBracketsClose()
        }

        rule<ArithmeticStatements.AdditionStatement>("addition_with_condition") {
            registerLikeArgument("output")
            literal("=")
            valueLikeArgument("input1")
            literal("+")
            valueLikeArgument("input2")

            conditionSubStatement()
        }
    }

    private fun ParserTransformation.Builder<*>.conditionSubStatement() {
        literal("if")
        registerLikeArgument("conditionSource")
        conditionArgument("condition")
        literal("0")
    }
}
