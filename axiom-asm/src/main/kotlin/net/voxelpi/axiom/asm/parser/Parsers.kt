package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.InstructionStatement
import net.voxelpi.axiom.asm.statement.ScopeStatement
import net.voxelpi.axiom.asm.statement.preprocessor.IncludeStatement
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public object Parsers {

    public val AXIOM_ASM: Parser = Parser.create {

        // INCLUDE

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

        // SCOPES

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

        // Instruction statements. These are generated twice, once with and once without the condition part.
        for (withConditionPart in listOf(true, false)) {
            val transformationSuffix = if (withConditionPart) "_with_condition" else "_without_condition"

            // LOAD
            transformation<InstructionStatement.WithOutput>("load${transformationSuffix}") {
                registerLikeArgument("output")
                literal("=")
                valueLikeArgument("inputA")

                generateCondition(withConditionPart)

                parameter("inputB") { 0 }
                parameter("operation") { Operation.LOAD }
            }

            // LOAD 2
            transformation<InstructionStatement.WithOutput>("load_2${transformationSuffix}") {
                registerLikeArgument("output")
                literal("=")
                valueLikeArgument("inputA")
                literal(",")
                valueLikeArgument("inputB")

                generateCondition(withConditionPart)

                parameter("operation") { Operation.LOAD_2 }
            }

            // COMMAND FUNCTIONS, "<function>"

            val commandFunctions = mapOf(
                "nop" to Operation.AND,
                "break" to Operation.BREAK,
            )

            for ((operator, operation) in commandFunctions) {
                transformation<InstructionStatement.WithOutput>("${operator}${transformationSuffix}") {
                    literal(operator)

                    generateCondition(withConditionPart)

                    parameter("inputA") { 0 }
                    parameter("inputB") { 0 }
                    parameter("operation") { operation }
                }
            }

            // BINARY OPERATORS, "R = A <operator> B"

            val abInputWithOutputOperators = mapOf(
                "and" to Operation.AND,
                "nand" to Operation.NAND,
                "or" to Operation.OR,
                "nor" to Operation.NOR,
                "xor" to Operation.XOR,
                "xnor" to Operation.XNOR,
                "+" to Operation.ADD,
                "-" to Operation.SUBTRACT,
                "*" to Operation.MULTIPLY,
                "/" to Operation.DIVIDE,
                "%" to Operation.MODULO,
                "bit get" to Operation.BIT_GET,
                "bit set" to Operation.BIT_SET,
                "bit clear" to Operation.BIT_CLEAR,
                "bit toggle" to Operation.BIT_TOGGLE,
            )

            for ((operator, operation) in abInputWithOutputOperators) {
                transformation<InstructionStatement.WithOutput>("${operator}${transformationSuffix}") {
                    registerLikeArgument("output")
                    literal("=")
                    valueLikeArgument("inputA")
                    literal(operator)
                    valueLikeArgument("inputB")

                    generateCondition(withConditionPart)

                    parameter("operation") { operation }
                }
            }

            // A -> R functions, "R = <function> A"

            val aInputWithOutputFunctions = mapOf(
                "shift left" to Operation.SHIFT_LEFT,
                "shift right" to Operation.SHIFT_RIGHT,
                "rotate left" to Operation.ROTATE_LEFT,
                "rotate right" to Operation.ROTATE_RIGHT,
            )

            for ((operator, operation) in aInputWithOutputFunctions) {
                transformation<InstructionStatement.WithOutput>("${operator}${transformationSuffix}") {
                    registerLikeArgument("output")
                    literal("=")
                    literal(operator)
                    valueLikeArgument("inputA")

                    generateCondition(withConditionPart)

                    parameter("operation") { operation }
                    parameter("inputB") { 0 }
                }
            }

            transformation<InstructionStatement.WithOutput>("increment${transformationSuffix}") {
                literal("inc")
                val register = registerLikeArgument("inputA")

                generateCondition(withConditionPart)

                parameter("operation") { Operation.ADD }
                parameter("inputB") { 1 }
                parameter("output") { this[register] }
            }

            transformation<InstructionStatement.WithOutput>("decrement${transformationSuffix}") {
                literal("dec")
                val register = registerLikeArgument("inputA")

                generateCondition(withConditionPart)

                parameter("operation") { Operation.SUBTRACT }
                parameter("inputB") { 1 }
                parameter("output") { this[register] }
            }
        }
    }

    private fun ParserTransformation.Builder<*>.generateCondition(withConditionPart: Boolean) {
        if (withConditionPart) {
            withCondition()
        } else {
            withoutCondition()
        }
    }

    private fun ParserTransformation.Builder<*>.withCondition() {
        literal("if")
        registerLikeArgument("conditionRegister")
        conditionArgument("condition")
        literal("0")
    }

    private fun ParserTransformation.Builder<*>.withoutCondition() {
        parameter("conditionRegister") { "R1" }
        parameter("condition") { Condition.ALWAYS }
    }
}
