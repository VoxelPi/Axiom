package net.voxelpi.axiom.emulator

public class EmulatedStack(public val capacity: Int) {

    private val data: ULongArray = ULongArray(capacity) { 0UL }
    private var pointer: Int = 0

    public val size: Int
        get() = pointer

    public fun push(value: ULong) {
        data[pointer] = value
        pointer++
        if (pointer >= capacity) {
            pointer = 0
        }
    }

    public fun pop(): ULong {
        pointer--
        if (pointer < 0) {
            pointer = capacity - 1
        }
        return data[pointer]
    }

    public fun peek(): ULong {
        return data[if (pointer == 0) capacity - 1 else pointer - 1]
    }
}
