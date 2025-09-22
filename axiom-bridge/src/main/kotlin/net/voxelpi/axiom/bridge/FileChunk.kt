package net.voxelpi.axiom.bridge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

@JvmRecord
public data class FileChunk(val index: Int, val hashBytes: ByteArray, val sizeBytes: ByteArray, val dataBytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileChunk

        if (index != other.index) return false
        if (!hashBytes.contentEquals(other.hashBytes)) return false
        if (!sizeBytes.contentEquals(other.sizeBytes)) return false
        if (!dataBytes.contentEquals(other.dataBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + hashBytes.contentHashCode()
        result = 31 * result + sizeBytes.contentHashCode()
        result = 31 * result + dataBytes.contentHashCode()
        return result
    }

    public companion object {

        public fun fromBytes(data: ByteArray, chunkSize: Int): List<FileChunk> {
            return data.toList().chunked(chunkSize).withIndex().map { (chunkIndex, chunkData) ->
                val dataBytes = chunkData.toByteArray()

                val sizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataBytes.size).array()

                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(sizeBytes)
                digest.update(dataBytes)
                val hashBytes = digest.digest()

                FileChunk(
                    chunkIndex,
                    hashBytes = hashBytes,
                    sizeBytes = sizeBytes,
                    dataBytes = dataBytes,
                )
            }
        }
    }
}
