package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.statement.annotation.StatementType
import net.voxelpi.axiom.asm.util.isInstanceOfType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

public class StatementPrototype<T : Any> internal constructor(
    public val id: String,
    public val type: KClass<T>,
    public val parameters: Map<String, StatementParameter<*>>,
) {

    public fun createInstance(
        scope: Scope,
        source: SourceLink,
        parameterValues: Map<String, Any?> = emptyMap(),
        parameterSources: Map<String, SourceLink> = emptyMap(),
    ): Result<StatementInstance<T>> {
        return runCatching {
            StatementInstance(this, scope, source, parameterValues, parameterSources)
        }
    }

    public fun createInstance(statement: T, scope: Scope, source: SourceLink): StatementInstance<T> {
        val parameterValues = parameters.values.associate { parameter ->
            parameter.id to statement::class.memberProperties.find { it.name == parameter.id }?.getter?.call(statement)
        }
        return StatementInstance(
            this,
            scope,
            source,
            parameterValues,
            emptyMap(),
        )
    }

    public fun isValidParameterValue(parameterId: String, value: Any?): Boolean {
        val parameter = parameters[parameterId] ?: return false
        return isInstanceOfType(value, parameter.type)
    }

    public companion object {

        public fun <T : Any> fromType(typeClass: KClass<T>): Result<StatementPrototype<T>> {
            val statementType = typeClass.findAnnotation<StatementType>()
                ?: return Result.failure(IllegalArgumentException("The class $typeClass is not annotated with @StatementTemplate."))
            val statementId = statementType.id

            // Check if the type is an object.
            typeClass.objectInstance?.let {
                return Result.success(StatementPrototype(statementId, typeClass, emptyMap()))
            }

            val primaryConstructor = typeClass.primaryConstructor
                ?: return Result.failure(IllegalArgumentException("No primary constructor found for statement class $typeClass."))

            val parameters = mutableMapOf<String, StatementParameter<*>>()
            for (parameter in primaryConstructor.parameters) {
                if (parameter.isOptional || parameter.isVararg) {
                    continue
                }

                val statementParameter = StatementParameter.create<Any?>(parameter)
                parameters[statementParameter.id] = statementParameter
            }

            val statement = StatementPrototype(statementId, typeClass, parameters)
            return Result.success(statement)
        }

        public inline fun <reified T : Any> fromType(): Result<StatementPrototype<T>> {
            return fromType(T::class)
        }
    }
}
