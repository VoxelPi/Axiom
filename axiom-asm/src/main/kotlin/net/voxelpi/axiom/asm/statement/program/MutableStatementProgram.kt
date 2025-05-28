package net.voxelpi.axiom.asm.statement.program

import net.voxelpi.axiom.asm.anchor.Anchor
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.scope.GlobalScope
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.statement.StatementInstance
import net.voxelpi.axiom.asm.statement.StatementParameter
import java.util.UUID
import kotlin.reflect.full.isSubclassOf

public class MutableStatementProgram(
    override val globalScope: GlobalScope,
    override val statements: MutableList<StatementInstance<*>>,
    override val scopes: MutableMap<UUID, Scope> = mutableMapOf(),
    override val anchors: MutableMap<UUID, Anchor> = mutableMapOf(),
) : StatementProgram {

    public constructor(
        globalScope: GlobalScope,
        statements: List<StatementInstance<*>>,
    ) : this(globalScope, statements.toMutableList(), mutableMapOf(), mutableMapOf())

    init {
        scopes[globalScope.uniqueId] = globalScope
    }

    public fun transform(transformation: suspend SequenceScope<StatementInstance<*>>.(statement: StatementInstance<*>) -> Unit): Result<Unit> {
        val previousStatements = statements.toList()
        statements.clear()

        try {
            val newStatements = sequence {
                for (previousStatement in previousStatements) {
                    this.transformation(previousStatement)
                }
            }.toList()
            statements.addAll(newStatements)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
        return Result.success(Unit)
    }

    public inline fun <reified T : Any> transformType(noinline transformation: suspend SequenceScope<StatementInstance<*>>.(statement: StatementInstance<out T>) -> Unit): Result<Unit> {
        return transform { statement ->
            if (statement.prototype.type.isSubclassOf(T::class)) {
                @Suppress("UNCHECKED_CAST")
                this.transformation(statement as StatementInstance<T>)
            } else {
                yield(statement)
            }
        }
    }

    public inline fun <reified T> transformArgumentsOfType(noinline transformation: (statement: StatementInstance<*>, parameter: StatementParameter<T>, value: T) -> Any?): Result<Unit> {
        return transform { statementInstance ->
            val parameterValues = statementInstance.parameterValues.mapValues { (parameterId, value) ->
                if (value is T) {
                    @Suppress("UNCHECKED_CAST")
                    val parameter = statementInstance.prototype.parameters[parameterId]!! as StatementParameter<T>
                    val newValue = transformation(statementInstance, parameter, value)
                    if (!statementInstance.prototype.isValidParameterValue(parameterId, newValue)) {
                        throw SourceCompilationException(
                            statementInstance.sourceOfOrDefault(parameterId),
                            "Invalid value \"${newValue}\" for parameter $parameterId of type ${statementInstance.prototype.parameters[parameterId]?.type}.",
                        )
                    }
                    newValue
                } else {
                    value
                }
            }
            yield(
                StatementInstance(
                    statementInstance.prototype,
                    statementInstance.scope,
                    statementInstance.source,
                    parameterValues,
                    statementInstance.parameterSources,
                )
            )
        }
    }
}
