package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.voxelpi.axiom.arch.Architecture
import net.voxelpi.axiom.bridge.util.inputChannel
import net.voxelpi.axiom.bridge.util.receiveArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

public class AxiomBridgeConnection(
    private val architecture: Architecture,
    private val port: SerialPort,
) : AutoCloseable {

    private val inputChannel = port.inputChannel()

    private val outputStream = port.outputStream

    override fun close() {
        port.closePort()
    }

    public fun sendPacket(payload: ByteArray) {
        // Calculate the size.
        val size = payload.size
        val sizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).array()

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

    public fun uploadProgram(data: ByteArray): Result<Unit> {
        val architectureProgramByteCount = architecture.programSize * architecture.instructionWordType.bytes.toULong()

        // Check program length.
        if (data.size.toULong() != architectureProgramByteCount) {
            return Result.failure(IllegalArgumentException("Invalid program size. Must be ${architecture.programSize}"))
        }

        // Split data into chunks.
        val chunks = data.toList().chunked(1024).map { it.toByteArray() }
        check(chunks.size.toULong() == architectureProgramByteCount / 1024UL) { "Data too long" }

        val chunkUsed = chunks.map { chunk -> !chunk.all { it == 0.toByte() } }

        // Send program header.
        if (true) {
            val packetBuffer = ByteBuffer.allocate(1 + (chunks.size / 8) + (if (chunks.size % 8 == 0) 0 else 1))
            packetBuffer.order(ByteOrder.LITTLE_ENDIAN)

            packetBuffer.put(PACKET_ID_UPLOAD_PROGRAM_START.toByte())

            val presentBitMaps = chunkUsed.chunked(64).map { chunkUsedChunk ->
                var chunkPresentData: ULong = 0UL
                for ((bit, used) in chunkUsedChunk.withIndex()) {
                    if (used) {
                        chunkPresentData = chunkPresentData or (1UL shl bit)
                    }
                }
                chunkPresentData.toLong()
            }
            for (bitMap in presentBitMaps) {
                packetBuffer.putLong(bitMap)
            }

            // Send the header packet.
            sendPacket(packetBuffer.array())
        }

        // Send chunk.
        for ((chunkIndex, chunk) in chunks.withIndex()) {
            if (!chunkUsed[chunkIndex]) {
                continue
            }

            val packetBuffer = ByteBuffer.allocate(1 + 2 + 1024)
            packetBuffer.order(ByteOrder.LITTLE_ENDIAN)

            packetBuffer.put(PACKET_ID_UPLOAD_PROGRAM_CHUNK.toByte())
            packetBuffer.putShort(chunkIndex.toShort())
            packetBuffer.put(chunk)

            // Send the chunk packet.
            sendPacket(packetBuffer.array())
        }

        // Send program end.
        val packetBuffer = ByteBuffer.allocate(1)
        packetBuffer.put(PACKET_ID_UPLOAD_PROGRAM_END.toByte())
        sendPacket(packetBuffer.array())

        // Wait for the response.
        runBlocking {
            delay(100)

            withTimeout(1000) {
                val responseArray = readPacket().getOrThrow()
                val responseBuffer = ByteBuffer.wrap(responseArray)
                responseBuffer.order(ByteOrder.LITTLE_ENDIAN)
                val id = responseBuffer.get()
                check(id == PACKET_ID_UPLOAD_PROGRAM_RESPONSE.toByte())

                val valid = responseBuffer.get() != 0.toByte()
                check(valid) { "Invalid chunk uploaded" }

                for (i in 0..<(chunks.size / 8)) {
                    val data = responseBuffer.get()
                    check(data == 0.toByte()) { "Chunk missing" }
                }
            }
        }

        return Result.success(Unit)
    }

    public companion object {
        private const val PACKET_ID_INFO = 0x01
        private const val PACKET_ID_UPLOAD_PROGRAM_START = 0x10
        private const val PACKET_ID_UPLOAD_PROGRAM_CHUNK = 0x11
        private const val PACKET_ID_UPLOAD_PROGRAM_END = 0x12
        private const val PACKET_ID_UPLOAD_PROGRAM_RESPONSE = 0x13
    }
}
