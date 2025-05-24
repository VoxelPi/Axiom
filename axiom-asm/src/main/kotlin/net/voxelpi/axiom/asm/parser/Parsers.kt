package net.voxelpi.axiom.asm.parser

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

        transformation("include/unit", IncludeStatement.IncludeUnit) {
            directive("include")
            unitArgument(IncludeStatement.Parameter.UNIT)
        }

        transformation("include/scope", IncludeStatement.IncludeScopeFromUnit) {
            directive("include")
            scopeNameArgument(IncludeStatement.Parameter.SCOPE)
            literal("from")
            unitArgument(IncludeStatement.Parameter.UNIT)
        }

        transformation("include/scope_with_alias", IncludeStatement.IncludeScopeFromUnitAsAlias) {
            directive("include")
            scopeNameArgument(IncludeStatement.Parameter.SCOPE)
            literal("from")
            unitArgument(IncludeStatement.Parameter.UNIT)
            literal("as")
            scopeNameArgument(IncludeStatement.Parameter.ALIAS)
        }

        // SCOPES

        transformation("scope/open_unnamed", ScopeStatement.OpenUnnamed) {
            curlyBracketsOpen()
        }

        transformation("scope/open_named", ScopeStatement.OpenNamed) {
            scopeNameArgument(ScopeStatement.Parameter.NAME)
            curlyBracketsOpen()
        }

        transformation("scope/close", ScopeStatement.Close) {
            curlyBracketsClose()
        }

        // Instruction statements. These are generated twice, once with and once without the condition part.
        for (withConditionPart in listOf(true, false)) {
            val transformationSuffix = if (withConditionPart) "with_condition" else "without_condition"

            // REPEAT
            transformation("repeat_parent_${transformationSuffix}", ScopeJumpStatement.Repeat) {
                literal("repeat")

                generateCondition(withConditionPart)

                parameter(ScopeJumpStatement.Parameter.SCOPE) { ScopeLike.ParentScope }
            }
            transformation("repeat_named_scope_${transformationSuffix}", ScopeJumpStatement.Repeat) {
                literal("repeat")
                scopeArgument(ScopeJumpStatement.Parameter.SCOPE)

                generateCondition(withConditionPart)
            }

            // EXIT
            transformation("exit_parent_${transformationSuffix}", ScopeJumpStatement.Exit) {
                literal("exit")

                generateCondition(withConditionPart)

                parameter(ScopeJumpStatement.Parameter.SCOPE) { ScopeLike.ParentScope }
            }
            transformation("exit_named_scope_${transformationSuffix}", ScopeJumpStatement.Exit) {
                literal("exit")
                scopeArgument(ScopeJumpStatement.Parameter.SCOPE)

                generateCondition(withConditionPart)
            }

            // LOAD
            transformation("load_${transformationSuffix}", InstructionStatement.InstructionWithOutput) {
                registerLikeArgument(InstructionStatement.Parameter.OUTPUT)
                literal("=")
                valueLikeArgument(InstructionStatement.Parameter.INPUT_A)

                generateCondition(withConditionPart)

                parameter(InstructionStatement.Parameter.INPUT_B) { IntegerValue(0) }
                parameter(InstructionStatement.Parameter.OPERATION) { Operation.LOAD }
            }

            // LOAD 2, 'PC = R1, R2'
            transformation("load_2_${transformationSuffix}", InstructionStatement.InstructionWithOutput) {
                registerLikeArgument(InstructionStatement.Parameter.OUTPUT)
                literal("=")
                valueLikeArgument(InstructionStatement.Parameter.INPUT_A)
                literal(",")
                valueLikeArgument(InstructionStatement.Parameter.INPUT_B)

                generateCondition(withConditionPart)

                parameter(InstructionStatement.Parameter.OPERATION) { Operation.LOAD_2 }
            }

            // COMMAND FUNCTIONS, "<function>"

            val commandFunctions = mapOf(
                "nop" to Operation.AND,
                "break" to Operation.BREAK,
            )

            for ((operator, operation) in commandFunctions) {
                transformation("${operator}_${transformationSuffix}", InstructionStatement.InstructionWithoutOutput) {
                    literal(operator)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement.Parameter.INPUT_A) { IntegerValue(0) }
                    parameter(InstructionStatement.Parameter.INPUT_B) { IntegerValue(0) }
                    parameter(InstructionStatement.Parameter.OPERATION) { operation }
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
                transformation("${operator}_${transformationSuffix}", InstructionStatement.InstructionWithOutput) {
                    registerLikeArgument(InstructionStatement.Parameter.OUTPUT)
                    literal("=")
                    valueLikeArgument(InstructionStatement.Parameter.INPUT_A)
                    literal(operator)
                    valueLikeArgument(InstructionStatement.Parameter.INPUT_B)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement.Parameter.OPERATION) { operation }
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
                transformation("${operator}_${transformationSuffix}", InstructionStatement.InstructionWithOutput) {
                    registerLikeArgument(InstructionStatement.Parameter.OUTPUT)
                    literal("=")
                    literal(operator)
                    valueLikeArgument(InstructionStatement.Parameter.INPUT_A)

                    generateCondition(withConditionPart)

                    parameter(InstructionStatement.Parameter.OPERATION) { operation }
                    parameter(InstructionStatement.Parameter.INPUT_B) { IntegerValue(0) }
                }
            }

            transformation("increment_${transformationSuffix}", InstructionStatement.InstructionWithOutput) {
                literal("inc")
                val register = registerLikeArgument(InstructionStatement.Parameter.INPUT_A)

                generateCondition(withConditionPart)

                parameter(InstructionStatement.Parameter.OPERATION) { Operation.ADD }
                parameter(InstructionStatement.Parameter.INPUT_B) { IntegerValue(1) }
                parameter(InstructionStatement.Parameter.OUTPUT) { this[register] }
            }

            transformation("decrement_${transformationSuffix}", InstructionStatement.InstructionWithOutput) {
                literal("dec")
                val register = registerLikeArgument(InstructionStatement.Parameter.INPUT_A)

                generateCondition(withConditionPart)

                parameter(InstructionStatement.Parameter.OPERATION) { Operation.SUBTRACT }
                parameter(InstructionStatement.Parameter.INPUT_B) { IntegerValue(1) }
                parameter(InstructionStatement.Parameter.OUTPUT) { this[register] }
            }
        }
    }

    private fun ParserTransformation.Builder.generateCondition(withConditionPart: Boolean) {
        if (withConditionPart) {
            withCondition()
        } else {
            withoutCondition()
        }
    }

    private fun ParserTransformation.Builder.withCondition() {
        literal("if")
        registerLikeArgument(InstructionStatement.Parameter.CONDITION_VALUE)
        conditionArgument(InstructionStatement.Parameter.CONDITION)
        literal("0")
    }

    private fun ParserTransformation.Builder.withoutCondition() {
        parameter(InstructionStatement.Parameter.CONDITION_VALUE) { RegisterLike.UnparsedRegister("R1") }
        parameter(InstructionStatement.Parameter.CONDITION) { Condition.ALWAYS }
    }
}
