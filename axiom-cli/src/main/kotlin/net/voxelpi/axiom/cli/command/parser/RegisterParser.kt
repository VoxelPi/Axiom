package net.voxelpi.axiom.cli.command.parser

import net.voxelpi.axiom.register.Register
import net.voxelpi.axiom.register.RegisterFile
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.BlockingSuggestionProvider

class RegisterParser<C : Any>(
    val registers: RegisterFile<*>,
) : ArgumentParser<C, Register<*>>, BlockingSuggestionProvider.Strings<C> {

    override fun parse(commandContext: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<Register<*>> {
        val input = commandInput.readString()

        val register = registers.register(input)
            ?: return ArgumentParseResult.failure(IllegalArgumentException("Unknown register \"$input\""))

        return ArgumentParseResult.success(register)
    }

    override fun stringSuggestions(commandContext: CommandContext<C?>, input: CommandInput): Iterable<String> {
        return registers.registers().map { it.id }
    }
}

fun <C : Any> registerParser(registers: RegisterFile<*>): ParserDescriptor<C, Register<*>> {
    return ParserDescriptor.of(
        RegisterParser<C>(registers),
        Register::class.java,
    )
}
