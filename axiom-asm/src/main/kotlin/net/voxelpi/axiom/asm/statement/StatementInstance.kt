package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.util.isInstanceOfType
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

public data class StatementInstance<T : Any>(
    public val prototype: StatementPrototype<T>,
    public val scope: Scope,
    public val source: SourceLink,
    public val parameterValues: Map<String, *>,
    public val parameterSources: Map<String, SourceLink>,
) {

    init {
        // Validate parameter types.
        for (parameter in prototype.parameters.values) {
            require(parameter.id in parameterValues) { "No value specified for parameter '${parameter.id}'." }

            val parameterValue = parameterValues[parameter.id]
            require(isInstanceOfType(parameterValue, parameter.type)) { "Invalid parameter value '$parameterValue' for parameter '${parameter.id}' of type ${parameter.type}" }
        }

        // Validate parameter values
        require(prototype.parameters.size == parameterValues.size) { "Invalid number of parameter." }

        // Try building the instance.
        build()
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(parameter: StatementParameter<T>): T {
        return parameterValues[parameter.id] as T
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T> get(property: KProperty<T>): T {
        return parameterValues[property.name] as T
    }

    public fun sourceOf(parameter: StatementParameter<*>): SourceLink? {
        return parameterSources[parameter.id]
    }

    public fun sourceOf(property: KProperty<*>): SourceLink? {
        return parameterSources[property.name]
    }

    public fun sourceOfOrDefault(parameter: StatementParameter<*>): SourceLink {
        return parameterSources[parameter.id] ?: source
    }

    public fun sourceOfOrDefault(property: KProperty<*>): SourceLink {
        return parameterSources[property.name] ?: source
    }

    public fun build(): T {
        prototype.type.objectInstance?.let {
            return it
        }

        // Get the primary constructor.
        val primaryConstructor = prototype.type.primaryConstructor
            ?: throw ParseException(source, "No primary constructor found for statement class ${prototype.type}.")

        // Create the statement.
        val constructorValues = primaryConstructor.parameters.associateWith { parameter ->
            parameterValues[parameter.name!!]
        }
        return primaryConstructor.callBy(constructorValues)
    }
}
