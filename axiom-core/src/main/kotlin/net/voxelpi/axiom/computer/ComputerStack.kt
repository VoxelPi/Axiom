package net.voxelpi.axiom.computer

public class ComputerStack(public val capacity: Int) {

    private val data: ULongArray = ULongArray(capacity) { 0UL }
    private var pointer: Int = 0

    public val size: Int
        get() = pointer

    public fun clear() {
        for (i in 0 until capacity) {
            data[i] = 0UL
        }
        pointer = 0
    }

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
