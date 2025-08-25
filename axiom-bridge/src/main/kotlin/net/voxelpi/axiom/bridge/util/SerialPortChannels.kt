package net.voxelpi.axiom.bridge.util

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking

internal fun SerialPort.inputChannel(bufferCapacity: Int = 4096): Channel<Byte> {
    val channel = Channel<Byte>(bufferCapacity)

    addDataListener(object : SerialPortDataListener {
        override fun getListeningEvents(): Int {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
        }

        override fun serialEvent(event: SerialPortEvent) {
            if (event.eventType != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                return
            }

            val available = event.serialPort.bytesAvailable()
            if (available <= 0) {
                return
            }

            val buffer = ByteArray(available)
            val read = event.serialPort.readBytes(buffer, buffer.size)
            if (read > 0) {
                for (i in 0 until read) {
                    channel.trySendBlocking(buffer[i])
                }
            }
        }
    })

    return channel
}
