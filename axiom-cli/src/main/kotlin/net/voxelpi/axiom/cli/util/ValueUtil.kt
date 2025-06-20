package net.voxelpi.axiom.cli.util

import com.github.ajalt.mordant.rendering.TextColors
import net.voxelpi.axiom.WordType
import kotlin.math.ceil
import kotlin.math.log10

fun visibleLength(s: String): Int {
    // Regex to match ANSI escape codes
    val ansiRegex = Regex("""\u001B\[[0-9;]*[mK]""")
    // Remove ANSI codes, then measure length
    return ansiRegex.replace(s, "").length
}

fun formattedBooleanValue(value: Boolean): String {
    return if (value) {
        TextColors.brightGreen("true")
    } else {
        TextColors.brightRed("false")
    }
}

fun formattedValue(value: ULong, type: WordType, format: ValueFormat): String {
    return when (format) {
        ValueFormat.BINARY -> {
            val state = type.unsignedValueOf(value)
            "${TextColors.brightCyan("0b")}${TextColors.brightGreen(state.toString(2).padStart(type.bits, '0').chunked(4).joinToString("_"))}"
        }
        ValueFormat.DECIMAL -> {
            val state = type.unsignedValueOf(value)
            val maxLength = ceil(type.bits * log10(2.0)).toInt()

            TextColors.brightGreen(state.toString().padStart(maxLength))
        }
        ValueFormat.HEXADECIMAL -> {
            val state = type.unsignedValueOf(value)
            "${TextColors.brightCyan("0x")}${TextColors.brightGreen(state.toString(16).padStart(type.bytes * 2, '0').chunked(2).joinToString("_").uppercase())}"
        }
        ValueFormat.DECIMAL_SIGNED -> {
            val state = type.signedValueOf(value)
            val maxLength = ceil((type.bits - 1) * log10(2.0)).toInt() + 1 // Plus one for optional negative sign.

            TextColors.brightGreen(state.toString().padStart(maxLength))
        }
        ValueFormat.CHARACTER -> {
            val state = type.unsignedValueOf(value)
            val symbol = stringFromCodePoint(state)
            "${TextColors.brightCyan("'")}${TextColors.brightGreen(symbol)}${TextColors.brightCyan("'")}"
        }
    }
}

fun codePointFromString(str: String): ULong {
    if (str.isEmpty()) {
        return (-1).toULong()
    }
    return str.codePointAt(0).toULong()
}

fun stringFromCodePoint(codePoint: ULong): String {
    return try {
        Character.toChars(codePoint.toUInt().toInt()).concatToString()
            .replace(0.toChar().toString(), "\\0")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace(12.toChar().toString(), "\\f")
            .replace(11.toChar().toString(), "\\v")
    } catch (_: Exception) {
        "INVALID"
    }
}

enum class ValueFormat {
    BINARY,
    DECIMAL,
    HEXADECIMAL,
    DECIMAL_SIGNED,
    CHARACTER,
}
