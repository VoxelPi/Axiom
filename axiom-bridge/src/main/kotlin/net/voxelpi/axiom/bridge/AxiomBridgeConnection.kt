package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort
import net.voxelpi.axiom.bridge.util.inputChannel
import net.voxelpi.axiom.bridge.util.receiveArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

public class AxiomBridgeConnection(private val port: SerialPort) : AutoCloseable {

    private val inputChannel = port.inputChannel()

    private val inputStream = port.inputStream

    override fun close() {
        port.closePort()
    }

    public fun sendPacket(payload: ByteArray) {

    }

    public suspend fun readPacket(): Result<ByteArray> = runCatching {
        // Read packet hash.
        val hashBytes = inputChannel.receiveArray(32)

        // Read packet size.
        val sizeBytes = inputChannel.receiveArray(4)
        val size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
        check(size >= 0) { "Invalid payload size $size" }

        // Read packet payload.
        val payload = inputChannel.receiveArray(size)

        // Compute the packet hash.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sizeBytes)
        digest.update(payload)
        val computedHashBytes = digest.digest()

        // Verify the packet hash.
        check(hashBytes.contentEquals(computedHashBytes)) { "Hash mismatch" }

        // Return the payload
        return@runCatching payload
    }
}
