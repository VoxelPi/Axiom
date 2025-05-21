package net.voxelpi.axiom.instruction

public enum class OperationType(public val inputA: Boolean, public val inputB: Boolean, public val output: Boolean) {
    NO_INPUT_NO_OUTPUT(false, false, false),
    NO_INPUT_WITH_OUTPUT(false, false, true),
    A_INPUT_NO_OUTPUT(true, false, false),
    A_INPUT_WITH_OUTPUT(true, false, true),
    B_INPUT_NO_OUTPUT(false, true, false),
    B_INPUT_WITH_OUTPUT(false, true, true),
    AB_INPUT_NO_OUTPUT(true, true, false),
    AB_INPUT_WITH_OUTPUT(true, true, true),
}
