package net.voxelpi.axiom.instruction

public enum class Condition(public val symbol: String) {
    ALWAYS("true"),
    NEVER("false"),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS("<"),
    GREATER_OR_EQUAL(">="),
    GREATER(">"),
    LESS_OR_EQUAL("<="),
    ;

    public fun inv(): Condition {
        return entries[ordinal xor 1]
    }
}
