package net.voxelpi.axiom.instruction

public enum class Condition(public val symbol: String) {
    ALWAYS("true"),
    NEVER("false"),
    EQUAL("="),
    NOT_EQUAL("!="),
    LESS("<"),
    LESS_OR_EQUAL("<="),
    GREATER(">"),
    GREATER_OR_EQUAL(">="),
}
