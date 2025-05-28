package net.voxelpi.axiom.emulator

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmulatedStackTest {

    @Test
    fun `test simple stack`() {
        val stack = EmulatedStack(4)
        stack.push(1UL)
        stack.push(2UL)

        assertEquals(2, stack.size)

        assertEquals(2UL, stack.pop())

        stack.push(3UL)
        stack.push(4UL)

        assertEquals(3, stack.size)
        assertEquals(4UL, stack.peek())
        assertEquals(3, stack.size)
        assertEquals(4UL, stack.pop())
        assertEquals(3UL, stack.pop())
    }
}
