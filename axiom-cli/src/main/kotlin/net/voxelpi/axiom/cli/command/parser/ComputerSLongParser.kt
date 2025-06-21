package net.voxelpi.axiom.cli.command.parser

import net.voxelpi.axiom.util.parseInteger
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor

class ComputerSLongParser<C : Any> : ArgumentParser<C, Long> {

    override fun parse(commandContext: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<Long> {
        val input = commandInput.readString()

        val number = parseInteger(input)
            ?: return ArgumentParseResult.failure(IllegalArgumentException("Invalid integer \"$input\""))

        return ArgumentParseResult.success(number)
    }
}

fun <C : Any> computerSLongParser(): ParserDescriptor<C, Long> {
    return ParserDescriptor.of(
        ComputerSLongParser<C>(),
        Long::class.java,
    )
}
