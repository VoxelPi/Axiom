package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.types.ConditionalStatement
import net.voxelpi.axiom.asm.statement.types.IncludeStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.statement.types.ScopeJumpStatement
import net.voxelpi.axiom.asm.statement.types.ScopeStatement
import net.voxelpi.axiom.asm.type.IntegerValue
import net.voxelpi.axiom.asm.type.RegisterLike
import net.voxelpi.axiom.asm.type.ScopeLike
import net.voxelpi.axiom.instruction.Condition
import net.voxelpi.axiom.instruction.Operation

public object Parsers {

    public val AXIOM_ASM: Parser = Parser.create {

        // INCLUDE

        transformation<IncludeStatement.Unit>("include/unit") {
            directive("include")
            unitArgument(IncludeStatement::unit)
        }

        transformation<IncludeStatement.Scope.Direct>("include/scope") {
            directive("include")
            scopeArgument(IncludeStatement.Scope::scope)
            literal("from")
            unitArgument(IncludeStatement::unit)
        }

        transformation<IncludeStatement.Scope.WithAlias>("include/scope_with_alias") {
            directive("include")
            scopeArgument(IncludeStatement.Scope::scope)
            literal("from")
            unitArgument(IncludeStatement::unit)
            literal("as")
            scopeArgument(IncludeStatement.Scope.WithAlias::alias)
        }

        // SCOPES

        transformation<ScopeStatement.Open.Unnamed>("scope/open_unnamed") {
            curlyBracketsOpen()
        }

        transformation<ScopeStatement.Open.Named>("scope/open_named") {
            scopeArgument(ScopeStatement.Open.Named::name)
            curlyBracketsOpen()
        }

        transformation<ScopeStatement.Close>("scope/close") {
            curlyBracketsClose()
        }

        // Instruction statements. These are generated twice, once with and once without the condition part.
        for (withConditionPart in listOf(true, false)) {
            val transformationSuffix = if (withConditionPart) "with_condition" else "without_condition"

            // REPEAT
            transformation<ScopeJumpStatement.Repeat>("repeat_parent_${transformationSuffix}") {
                literal("repeat")

                generateCondition(withConditionPart)

                parameter(ScopeJumpStatement::scope) { ScopeLike.ParentScope }
            }
            transformation<ScopeJumpStatement.Repeat>("repeat_named_scope_${transformationSuffix}") {
                literal("repeat")
                scopeArgument(ScopeJumpStatement::scope)

                generateCondition(withConditionPart)
            }

            // EXIT
            transformation<ScopeJumpStatement.Exit>("exit_parent_${transformationSuffix}") {
                literal("exit")

                generateCondition(withConditionPart)

                parameter(ScopeJumpStatement::scope) { ScopeLike.ParentScope }
            }
            transformation<ScopeJumpStatement.Exit>("exit_named_scope_${transformationSuffix}") {
                literal("exit")
                scopeArgument(ScopeJumpStatement::scope)

                generateCondition(withConditionPart)
            }

            // LOAD
            transformation<InstructionStatement.WithOutput>("load_${transformationSuffix}") {
                registerLikeArgument(InstructionStatement.WithOutput::output)
                literal("=")
                valueLikeArgument(InstructionStatement::inputA)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::inputB) { IntegerValue(0) }
                parameter(InstructionStatement::operation) { Operation.LOAD }
            }

            // LOAD 2, 'PC = R1, R2'
            transformation<InstructionStatement.WithOutput>("load_2_${transformationSuffix}") {
                registerLikeArgument(InstructionStatement.WithOutput::output)
                literal("=")
                valueLikeArgument(InstructionStatement::inputA)
                literal(",")
                valueLikeArgument(InstructionStatement::inputB)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.LOAD_2 }
            }

            // COMMAND FUNCTIONS, "<function>"

            val commandFunctions = mapOf(
                "nop" to Operation.AND,
                "break" to Operation.BREAK,
            )

            for ((operator, operation) in commandFunctions) {
                transformation<InstructionStatement.WithoutOutput>("${operator}_${transformationSuffix}") {
                    literal(operator)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement::inputA) { IntegerValue(0) }
                    parameter(InstructionStatement::inputB) { IntegerValue(0) }
                    parameter(InstructionStatement::operation) { operation }
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
                transformation<InstructionStatement.WithOutput>("${operator}_${transformationSuffix}") {
                    registerLikeArgument(InstructionStatement.WithOutput::output)
                    literal("=")
                    valueLikeArgument(InstructionStatement::inputA)
                    literal(operator)
                    valueLikeArgument(InstructionStatement::inputB)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement::operation) { operation }
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
                transformation<InstructionStatement.WithOutput>("${operator}_${transformationSuffix}") {
                    registerLikeArgument(InstructionStatement.WithOutput::output)
                    literal("=")
                    literal(operator)
                    valueLikeArgument(InstructionStatement::inputA)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement::operation) { operation }
                    parameter(InstructionStatement::inputB) { IntegerValue(0) }
                }
            }

            transformation<InstructionStatement.WithOutput>("increment_${transformationSuffix}") {
                literal("inc")
                val register = registerLikeArgument(InstructionStatement.WithOutput::output)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.ADD }
                parameter(InstructionStatement::inputA) { this[register] }
                parameter(InstructionStatement::inputB) { IntegerValue(1) }
            }

            transformation<InstructionStatement.WithOutput>("decrement_${transformationSuffix}") {
                literal("dec")
                val register = registerLikeArgument(InstructionStatement.WithOutput::output)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.SUBTRACT }
                parameter(InstructionStatement::inputA) { this[register] }
                parameter(InstructionStatement::inputB) { IntegerValue(1) }
            }
        }
    }

    private fun ParserTransformation.Builder<out ConditionalStatement>.generateCondition(withConditionPart: Boolean) {
        if (withConditionPart) {
            withCondition()
        } else {
            withoutCondition()
        }
    }

    private fun ParserTransformation.Builder<out ConditionalStatement>.withCondition() {
        literal("if")
        registerLikeArgument(ConditionalStatement::conditionValue)
        conditionArgument(ConditionalStatement::condition)
        literal("0")
    }

    private fun ParserTransformation.Builder<out ConditionalStatement>.withoutCondition() {
        parameter(ConditionalStatement::conditionValue) { RegisterLike.UnparsedRegister("R1") }
        parameter(ConditionalStatement::condition) { Condition.ALWAYS }
    }
}
