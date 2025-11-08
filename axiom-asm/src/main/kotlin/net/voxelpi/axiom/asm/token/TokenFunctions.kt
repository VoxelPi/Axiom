package net.voxelpi.axiom.asm.token

internal fun <T : Token> List<T>.split(predicate: (T) -> Boolean): List<List<T>> {
    val entries = mutableListOf<List<T>>()
    val currentEntry = mutableListOf<T>()

    for (token in this) {
        if (predicate(token)) {
            entries.add(currentEntry.toList())
            currentEntry.clear()
        } else {
            currentEntry.add(token)
        }
    }
    entries.add(currentEntry.toList())
    currentEntry.clear()
    return entries
}

internal inline fun <reified T : Token> List<Token>.isEmptyOrType(): Boolean {
    return isEmpty() || all { it is T }
}

internal inline fun <reified T : Token, reified S : T> List<T>.trim(): List<T> {
    if (isEmpty()) {
        return emptyList()
    }

    val iFirst = indexOfFirst { it !is S }
    val iLast = indexOfLast { it !is S }
    if (iFirst == -1 || iLast == -1) {
        return emptyList()
    }
    return subList(iFirst, iLast + 1)
}
