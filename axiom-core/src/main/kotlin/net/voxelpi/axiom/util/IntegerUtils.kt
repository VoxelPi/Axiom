package net.voxelpi.axiom.util

public fun parseInteger(text: String): Long? {
    // Base-16 integer.
    if ("^[+-]?(0[xX][0-9a-fA-F_]+)$".toRegex().matches(text)) {
        val (explicitSign, sign) = parseNumberSign(text)
        val start = if (explicitSign) 3 else 2
        val integerText = text.substring(start).replace("_", "")
        val integer = integerText.toLong(16) * sign
        return integer
    }

    // Base-10 integer.
    if ("^[+-]?(0[dD]?[0-9_]+)$".toRegex().matches(text)) {
        val (explicitSign, sign) = parseNumberSign(text)
        val start = if (explicitSign) 3 else 2
        val integerText = text.substring(start).replace("_", "")
        val integer = integerText.toLong(10) * sign
        return integer
    }

    // Base-8 integer.
    if ("^[+-]?(0[oO][0-7_]+)$".toRegex().matches(text)) {
        val (explicitSign, sign) = parseNumberSign(text)
        val start = if (explicitSign) 3 else 2
        val integerText = text.substring(start).replace("_", "")
        val integer = integerText.toLong(8) * sign
        return integer
    }

    // Base-2 integer.
    if ("^[+-]?(0[bB][01_]+)$".toRegex().matches(text)) {
        val (explicitSign, sign) = parseNumberSign(text)
        val start = if (explicitSign) 3 else 2
        val integerText = text.substring(start).replace("_", "")
        val integer = integerText.toLong(2) * sign
        return integer
    }

    // Base-10 integer (implicit).
    if ("^[+-]?([0-9][0-9_]*)$".toRegex().matches(text)) {
        val (explicitSign, sign) = parseNumberSign(text)
        val start = if (explicitSign) 1 else 0
        val integerText = text.substring(start).replace("_", "")
        val integer = integerText.toLong(10) * sign
        return integer
    }

    return null
}

private fun parseNumberSign(text: String): Pair<Boolean, Int> {
    if (text.startsWith("-")) {
        return Pair(true, -1)
    }
    if (text.startsWith("+")) {
        return Pair(true, 1)
    }
    return Pair(false, 1)
}
