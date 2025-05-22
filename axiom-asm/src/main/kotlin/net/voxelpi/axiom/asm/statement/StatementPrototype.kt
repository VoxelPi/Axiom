package net.voxelpi.axiom.asm.statement

import net.voxelpi.axiom.asm.parser.exception.ParseException
import net.voxelpi.axiom.asm.scope.Scope
import net.voxelpi.axiom.asm.source.SourceLink
import net.voxelpi.axiom.asm.util.isInstanceOfType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

public data class StatementPrototype<out S : Statement>(
    override val source: SourceLink,
    val type: KClass<out S>,
    val scope: Scope,
    val arguments: MutableMap<String, *>,
) : Statement {

    public fun build(): Result<S> {
        // Build a map containing all constructor arguments.
        val constructorParameterValues = mutableMapOf<String, Any?>("source" to source)
        constructorParameterValues.putAll(arguments)

        // Get the primary constructor.
        val primaryConstructor = type.primaryConstructor
            ?: return Result.failure(ParseException(source, "No primary constructor found for statement class $type."))

        // Get constructor arguments of the primary constructor.
        val constructorParameterTypes = primaryConstructor.parameters
            .filterNot { it.isOptional || it.isVararg }
            .associate { parameter ->
                val name = parameter.name ?: return Result.failure(ParseException(source, "Constructor parameter '$parameter' has no name."))
                val type = parameter.type
                name to type
            }

        // Check the argument types.
        for ((parameterName, parameterType) in constructorParameterTypes) {
            // Check that the argument is present in the value map.
            if (parameterName !in constructorParameterValues) {
                return Result.failure(ParseException(source, "No value specified for argument $parameterName."))
            }
            val parameterValue = constructorParameterValues[parameterName]

            // Check that the type is valid.
            if (!isInstanceOfType(parameterValue, parameterType)) {
                val source = if (parameterValue is StatementArgument<*>) parameterValue.source else source
                return Result.failure(ParseException(source, "Value '$parameterValue' for argument $parameterName is not of type $parameterType"))
            }

            // Check that type of argument is valid.
            if (parameterType.classifier == StatementArgument::class && parameterValue is StatementArgument<*>) {
                val argumentType = parameterValue.type
                val parameterGenericType = parameterType.arguments.first().type ?: typeOf<Any?>()
                if (!argumentType.isSubtypeOf(parameterGenericType)) {
                    return Result.failure(ParseException(parameterValue.source, "Value '${parameterValue.value}' for argument $parameterName is not of type $parameterGenericType"))
                }
            }
        }

        // Create the statement.
        val constructorValues = primaryConstructor.parameters.associateWith { parameter ->
            constructorParameterValues[parameter.name!!]
        }
        return runCatching { primaryConstructor.callBy(constructorValues) }
    }
}
