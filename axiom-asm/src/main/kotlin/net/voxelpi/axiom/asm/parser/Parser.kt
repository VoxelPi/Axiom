package net.voxelpi.axiom.asm.parser

import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.lexer.TokenizedStatement
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementPrototype
import kotlin.reflect.KClass

public class Parser(
    public val transformations: List<ParserTransformation<*>>,
) {

    public fun parse(statement: TokenizedStatement, scope: Scope): Result<StatementInstance<*>> {
        for (transformation in transformations) {
            if (!transformation.isApplicable(statement)) {
                continue
            }

            val statement = transformation.apply(statement, scope)
            return statement
        }

        return Result.failure(ParseException(statement.source, "No applicable parsing rule found."))
    }

    public companion object {

        public fun create(block: Builder.() -> Unit): Parser {
            val builder = Builder()
            builder.block()
            return Parser(builder.transformations())
        }
    }

    public class Builder internal constructor() {

        private val transformations: MutableList<ParserTransformation<*>> = mutableListOf()

        public fun transformations(): List<ParserTransformation<*>> {
            return transformations
        }

        public inline fun <reified T : Any> transformation(id: String, noinline block: ParserTransformation.Builder<T>.() -> Unit): ParserTransformation<T> {
            return transformation(id, T::class, block)
        }

        public fun <T : Any> transformation(id: String, statementType: KClass<T>, block: ParserTransformation.Builder<T>.() -> Unit): ParserTransformation<T> {
            return transformation(id, StatementPrototype.fromType(statementType).getOrThrow(), block)
        }

        public fun <T : Any> transformation(id: String, statement: StatementPrototype<T>, block: ParserTransformation.Builder<T>.() -> Unit): ParserTransformation<T> {
            val transformation = ParserTransformation.create(id, statement, block)
            transformations += transformation
            return transformation
        }
    }
}
