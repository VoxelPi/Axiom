package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

public class AxiomBridgeConnection(private val port: SerialPort) : AutoCloseable {

    init {
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
    }

    private val inputStream = port.inputStream

    override fun close() {
        port.closePort()
    }

    public fun sendPacket(payload: ByteArray) {

    }

    public suspend fun readPacket(): Result<ByteArray> {
        // Read packet hash.
        val hashBytes = readBytes(32).getOrElse {
            return Result.failure(it)
        }

        // Read packet size.
        val sizeBytes = readBytes(4).getOrElse {
            return Result.failure(it)
        }
        val size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
        if (size < 0) {
            throw IllegalStateException("Invalid payload size")
        }

        // Read packet payload.
        val payload = readBytes(size).getOrElse {
            return Result.failure(it)
        }

        // Compute the packet hash.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sizeBytes)
        digest.update(payload)
        val computedHash = digest.digest()

        // Verify the packet hash.
        if (!hashBytes.contentEquals(computedHash)) {
            return Result.failure(IllegalStateException("Hash mismatch"))
        }

        // Return the payload
        return Result.success(payload)
    }

    private suspend fun readBytes(nBytes: Int): Result<ByteArray> = withContext(Dispatchers.IO) {
        val buffer = ByteArray(nBytes)
        var offset = 0

        while (offset < nBytes) {
            val read = inputStream.read(buffer, offset, nBytes - offset)
            if (read == -1) {
                return@withContext Result.failure(IllegalStateException("Stream closed while reading"))
            }
            offset += read
        }

        return@withContext Result.success(buffer)
    }
}
