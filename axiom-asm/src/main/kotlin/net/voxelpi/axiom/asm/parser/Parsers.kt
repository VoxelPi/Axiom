package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.statement.types.ConditionalStatement
import net.voxelpi.axiom.asm.statement.types.IfStatement
import net.voxelpi.axiom.asm.statement.types.IncludeStatement
import net.voxelpi.axiom.asm.statement.types.InstructionStatement
import net.voxelpi.axiom.asm.statement.types.LabelStatement
import net.voxelpi.axiom.asm.statement.types.ScopeJumpStatement
import net.voxelpi.axiom.asm.statement.types.ScopeStatement
import net.voxelpi.axiom.asm.statement.types.VariableStatement
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

        // Labels

        transformation<LabelStatement.Definition>("label") {
            labelArgument(LabelStatement.Definition::name)
        }

        // Variables

        transformation<VariableStatement.Definition>("variable") {
            variableArgument(VariableStatement.Definition::name)
            literal(":=")
            valueLikeArgument(VariableStatement.Definition::value)
        }

        // IF STATEMENTS, 'if <condition_register> <condition> 0'

        transformation<IfStatement>("if") {
            literal("if")
            registerLikeArgument(IfStatement::conditionValue)
            conditionArgument(IfStatement::condition)
            literal("0")
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

            // JUMP, 'jump <value>'
            transformation<InstructionStatement.WithOutput>("jump_${transformationSuffix}") {
                literal("jump")
                valueLikeArgument(InstructionStatement::inputA)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::inputB) { IntegerValue(0) }
                parameter(InstructionStatement::operation) { Operation.LOAD }
                parameter(InstructionStatement.WithOutput::output) { RegisterLike.PC }
            }

            // JUMP 2, 'jump <value1>, <value2>'
            transformation<InstructionStatement.WithOutput>("jump_2_${transformationSuffix}") {
                literal("jump")
                valueLikeArgument(InstructionStatement::inputA)
                literal(",")
                valueLikeArgument(InstructionStatement::inputB)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.LOAD_2 }
                parameter(InstructionStatement.WithOutput::output) { RegisterLike.PC }
            }

            // SKIP, 'skip <value>'
            transformation<InstructionStatement.WithOutput>("skip_${transformationSuffix}") {
                literal("skip")
                valueLikeArgument(InstructionStatement::inputA)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::inputB) { IntegerValue(0) }
                parameter(InstructionStatement::operation) { Operation.ADD }
                parameter(InstructionStatement.WithOutput::output) { RegisterLike.PC }
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

            val noInputWithOutputFunctions = mapOf(
                "read" to Operation.IO_READ,
                "poll" to Operation.IO_POLL,
            )

            for ((operator, operation) in noInputWithOutputFunctions) {
                transformation<InstructionStatement.WithOutput>("${operator}_${transformationSuffix}") {
                    registerLikeArgument(InstructionStatement.WithOutput::output)
                    literal("=")
                    literal(operator)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement::operation) { operation }
                    parameter(InstructionStatement::inputA) { IntegerValue(0) }
                    parameter(InstructionStatement::inputB) { IntegerValue(0) }
                }
            }

            val aInputNoOutputFunctions = mapOf(
                "write" to Operation.IO_WRITE,
            )

            for ((operator, operation) in aInputNoOutputFunctions) {
                transformation<InstructionStatement.WithoutOutput>("${operator}_${transformationSuffix}") {
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

            transformation<InstructionStatement.WithOutput>("call_${transformationSuffix}") {
                literal("call")
                valueLikeArgument(InstructionStatement::inputA)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.CALL }
                parameter(InstructionStatement::inputB) { IntegerValue(0) }
                parameter(InstructionStatement.WithOutput::output) { RegisterLike.PC }
            }

            transformation<InstructionStatement.WithOutput>("call_2_${transformationSuffix}") {
                literal("call")
                valueLikeArgument(InstructionStatement::inputA)
                literal(",")
                valueLikeArgument(InstructionStatement::inputB)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.CALL }
                parameter(InstructionStatement.WithOutput::output) { RegisterLike.PC }
            }

            transformation<InstructionStatement.WithOutput>("return_${transformationSuffix}") {
                literal("return")

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.RETURN }
                parameter(InstructionStatement::inputA) { IntegerValue(0) }
                parameter(InstructionStatement::inputB) { IntegerValue(0) }
                parameter(InstructionStatement.WithOutput::output) { RegisterLike.PC }
            }

            // LOAD, '<register> = <value>'
            transformation<InstructionStatement.WithOutput>("load_${transformationSuffix}") {
                registerLikeArgument(InstructionStatement.WithOutput::output)
                literal("=")
                valueLikeArgument(InstructionStatement::inputA)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::inputB) { IntegerValue(0) }
                parameter(InstructionStatement::operation) { Operation.LOAD }
            }

            // LOAD 2, '<register> = <value1>, <value2>'
            transformation<InstructionStatement.WithOutput>("load_2_${transformationSuffix}") {
                registerLikeArgument(InstructionStatement.WithOutput::output)
                literal("=")
                valueLikeArgument(InstructionStatement::inputA)
                literal(",")
                valueLikeArgument(InstructionStatement::inputB)

                generateCondition(withConditionPart)

                parameter(InstructionStatement::operation) { Operation.LOAD_2 }
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
        parameter(ConditionalStatement::conditionValue) { RegisterLike.AnyRegister(conditionable = true) }
        parameter(ConditionalStatement::condition) { Condition.ALWAYS }
    }
}
