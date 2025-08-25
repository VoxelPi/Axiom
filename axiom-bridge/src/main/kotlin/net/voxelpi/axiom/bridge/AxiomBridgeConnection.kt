package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort
import net.voxelpi.axiom.bridge.util.inputChannel
import net.voxelpi.axiom.bridge.util.receiveArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

public class AxiomBridgeConnection(private val port: SerialPort) : AutoCloseable {

    private val inputChannel = port.inputChannel()

    private val outputStream = port.outputStream

    override fun close() {
        port.closePort()
    }

    public fun sendPacket(payload: ByteArray) {
        // Calculate the size.
        val size = payload.size
        val sizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).put(payload).array()

        // Compute the packet hash.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sizeBytes)
        digest.update(payload)
        val hashBytes = digest.digest()

        // Send the packet.
        outputStream.write(hashBytes)
        outputStream.write(sizeBytes)
        outputStream.write(payload)
        outputStream.flush()
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
