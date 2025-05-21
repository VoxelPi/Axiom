package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.ScopeStatement
import net.voxelpi.axiom.asm.statement.instruction.ArithmeticStatements
import net.voxelpi.axiom.asm.statement.preprocessor.IncludeStatement

public object Parsers {

    public val AXIOM_ASM: Parser = Parser.create {
        transformation<IncludeStatement.Unit>("include/unit") {
            directive("include")
            unitArgument("unit")
        }

        transformation<IncludeStatement.Scope>("include/scope") {
            directive("include")
            scopeArgument("scope")
            literal("from")
            unitArgument("unit")
        }

        transformation<IncludeStatement.ScopeWithAlias>("include/scope_with_alias") {
            directive("include")
            scopeArgument("scope")
            literal("from")
            unitArgument("unit")
            literal("as")
            scopeArgument("alias")
        }

        transformation<ScopeStatement.OpenScope.Unnamed>("scope_open_unnamed") {
            curlyBracketsOpen()
        }

        transformation<ScopeStatement.OpenScope.Named>("scope_open_named") {
            scopeArgument("name")
            curlyBracketsOpen()
        }

        transformation<ScopeStatement.CloseScope>("scope_close") {
            curlyBracketsClose()
        }

        transformation<ArithmeticStatements.AdditionStatement>("addition_with_condition") {
            registerLikeArgument("output")
            literal("=")
            valueLikeArgument("input1")
            literal("+")
            valueLikeArgument("input2")

            conditionSubStatement()
        }

        transformation<ArithmeticStatements.AdditionStatement>("increment_with_condition") {
            literal("inc")
            val register = registerLikeArgument("input1")

            conditionSubStatement()

            parameter("input2") { 0 }
            parameter("output") { this[register] }
        }
    }

    private fun ParserTransformation.Builder<*>.conditionSubStatement() {
        literal("if")
        registerLikeArgument("conditionRegister")
        conditionArgument("condition")
        literal("0")
    }
}
