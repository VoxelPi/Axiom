package net.voxelpi.axiom.bridge.util

import kotlinx.coroutines.channels.Channel

internal suspend fun Channel<Byte>.receiveArray(n: Int): ByteArray {
    val buffer = ByteArray(n)
    for (i in buffer.indices) {
        buffer[i] = receive()
    }
    return buffer
}
