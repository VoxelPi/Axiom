package net.voxelpi.axiom.instruction

import net.voxelpi.axiom.register.RegisterVariable

public enum class Operation(public val type: OperationType, public val format: String) {
    CLEAR(OperationType.NO_INPUT_NO_OUTPUT, "{out} = clear"),
    LOAD(OperationType.A_INPUT_WITH_OUTPUT, "{out} = {a}"),
    LOAD_2(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {b}, {a}"),
    AND(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} and {b}"),
    NAND(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} nand {b}"),
    OR(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} or {b}"),
    NOR(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} nor {b}"),
    XOR(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} xor {b}"),
    XNOR(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} xnor {b}"),
    ADD(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} + {b}"),
    SUBTRACT(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} - {b}"),
    ADD_WITH_CARRY(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} + {b} with carry"),
    SUBTRACT_WITH_CARRY(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} - {b} with carry"),
    INCREMENT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = {a} + 1"),
    DECREMENT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = {a} - 1"),
    MULTIPLY(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} * {b}"),
    DIVIDE(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} / {b}"),
    MODULO(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} % {b}"),
    SQRT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = sqrt {a}"),
    SHIFT_LEFT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = shift left {a}"),
    SHIFT_RIGHT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = shift right {a}"),
    ROTATE_LEFT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = rotate left {a}"),
    ROTATE_RIGHT(OperationType.A_INPUT_WITH_OUTPUT, "{out} = rotate right {a}"),
    BIT_DECODE(OperationType.A_INPUT_WITH_OUTPUT, "{out} = decode {a}"),
    BIT_DECODE_INVERTED(OperationType.A_INPUT_WITH_OUTPUT, "{out} = ndecode {a}"),
    BIT_GET(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} bit get {b}"),
    BIT_SET(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} bit set {b}"),
    BIT_CLEAR(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} bit clear {b}"),
    BIT_TOGGLE(OperationType.AB_INPUT_WITH_OUTPUT, "{out} = {a} bit toggle {b}"),
    MEMORY_LOAD(OperationType.A_INPUT_WITH_OUTPUT, "{out} = [{a}]"),
    MEMORY_STORE(OperationType.AB_INPUT_NO_OUTPUT, "[{a}] = {b}"),
    IO_POLL(OperationType.NO_INPUT_WITH_OUTPUT, "{out} = poll"),
    IO_READ(OperationType.NO_INPUT_WITH_OUTPUT, "{out} = read"),
    IO_WRITE(OperationType.A_INPUT_NO_OUTPUT, "write {a}"),
    CALL(OperationType.A_INPUT_WITH_OUTPUT, "call {a}"),
    CALL_2(OperationType.AB_INPUT_WITH_OUTPUT, "call {b}, {a}"),
    RETURN(OperationType.NO_INPUT_WITH_OUTPUT, "return"),
    STACK_PUSH(OperationType.A_INPUT_NO_OUTPUT, "push {a}"),
    STACK_POP(OperationType.NO_INPUT_WITH_OUTPUT, "{out} = pop"),
    STACK_PEEK(OperationType.NO_INPUT_WITH_OUTPUT, "{out} = peek"),
    BREAK(OperationType.NO_INPUT_NO_OUTPUT, "break"),
    ;

    public fun asString(output: RegisterVariable, a: InstructionValue, b: InstructionValue): String {
        return format
            .replace("{out}", output.id)
            .replace("{a}", a.toString())
            .replace("{b}", b.toString())
    }

    public fun asString(output: String, a: String, b: String): String {
        return format
            .replace("{out}", output)
            .replace("{a}", a)
            .replace("{b}", b)
    }
}
