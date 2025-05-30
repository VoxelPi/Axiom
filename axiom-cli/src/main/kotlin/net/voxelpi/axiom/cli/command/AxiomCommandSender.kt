package net.voxelpi.axiom.cli.command

import org.jline.reader.LineReader
import org.jline.terminal.Terminal

data class AxiomCommandSender(
    val terminal: Terminal,
    val lineReader: LineReader,
)
