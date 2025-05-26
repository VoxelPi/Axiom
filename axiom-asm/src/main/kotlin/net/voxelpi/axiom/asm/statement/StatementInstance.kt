package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.exception.ParseException
import net.voxelpi.axiom.asm.exception.SourceCompilationException
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.util.isInstanceOfType
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

public data class StatementInstance<S : Any>(
    public val prototype: StatementPrototype<S>,
    public val scope: Scope,
    public val source: SourceLink,
    public val parameterValues: Map<String, Any?>,
    public val parameterSources: Map<String, SourceLink>,
) {

    init {
        // Validate parameter types.
        for (parameter in prototype.parameters.values) {
            if (parameter.id !in parameterValues) {
                throw SourceCompilationException(source, "No value specified for parameter '${parameter.id}'.")
            }

            val parameterValue = parameterValues[parameter.id]
            if (!isInstanceOfType(parameterValue, parameter.type)) {
                throw SourceCompilationException(source, "Invalid parameter value '$parameterValue' for parameter '${parameter.id}' of type ${parameter.type}.")
            }
        }

        // Validate parameter values
        require(prototype.parameters.size == parameterValues.size) { "Invalid number of parameter." }

        // Try building the instance.
        create()
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

    public fun sourceOfOrDefault(propertyId: String): SourceLink {
        return parameterSources[propertyId] ?: source
    }

    public fun create(): S {
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

    public fun modifiedCopy(block: Builder<S>.() -> Unit): StatementInstance<S> {
        val builder = Builder(prototype, scope, source, parameterValues.toMutableMap(), parameterSources.toMutableMap())
        builder.block()
        return builder.build()
    }

    public class Builder<S : Any> internal constructor(
        public val prototype: StatementPrototype<S>,
        public var scope: Scope,
        public var source: SourceLink,
        public val parameterValues: MutableMap<String, Any?>,
        public val parameterSources: MutableMap<String, SourceLink>,
    ) {
        @Suppress("UNCHECKED_CAST")
        public operator fun <T> get(parameter: StatementParameter<T>): T {
            return parameterValues[parameter.id] as T
        }

        @Suppress("UNCHECKED_CAST")
        public operator fun <T> get(property: KProperty<T>): T {
            return parameterValues[property.name] as T
        }

        @Suppress("UNCHECKED_CAST")
        public operator fun <T> set(parameter: StatementParameter<T>, value: T) {
            parameterValues[parameter.id] = value
        }

        @Suppress("UNCHECKED_CAST")
        public operator fun <T> set(property: KProperty<T>, value: T) {
            parameterValues[property.name] = value
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

        public fun build(): StatementInstance<S> {
            return StatementInstance(prototype, scope, source, parameterValues, parameterSources)
        }
    }
}
