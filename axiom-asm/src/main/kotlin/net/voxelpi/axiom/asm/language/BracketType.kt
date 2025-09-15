package net.voxelpi.axiom.asm.language

public enum class BracketType(public val openingSymbol: String, public val closingSymbol: String) {
    ROUND("(", ")"),
    SQUARE("[", "]"),
    CURLY("{", "}"),
}
