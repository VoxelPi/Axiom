package net.voxelpi.axiom.cli.command.parser

import net.voxelpi.axiom.util.parseInteger
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor

class ComputerULongParser<C : Any> : ArgumentParser<C, ULong> {

    override fun parse(commandContext: CommandContext<C>, commandInput: CommandInput): ArgumentParseResult<ULong> {
        val input = commandInput.readString()

        val number = parseInteger(input)?.toULong()
            ?: return ArgumentParseResult.failure(IllegalArgumentException("Invalid integer \"$input\""))

        return ArgumentParseResult.success(number)
    }
}

fun <C : Any> computerULongParser(): ParserDescriptor<C, ULong> {
    return ParserDescriptor.of(
        ComputerULongParser<C>(),
        ULong::class.java,
    )
}
