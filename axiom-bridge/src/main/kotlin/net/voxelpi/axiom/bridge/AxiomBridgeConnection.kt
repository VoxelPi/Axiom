package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort

public class AxiomBridgeConnection(private val port: SerialPort) : AutoCloseable {

    override fun close() {
        port.closePort()
    }
}
