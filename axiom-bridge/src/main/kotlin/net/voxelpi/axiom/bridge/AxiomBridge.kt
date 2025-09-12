package net.voxelpi.axiom.bridge

import com.fazecast.jSerialComm.SerialPort
import net.voxelpi.axiom.arch.Architecture

public object AxiomBridge {

    /**
     * Connects to the bridge at the specified [portDescriptor] with the given [baudRate].
     */
    public fun connect(portDescriptor: String, baudRate: Int, architecture: Architecture): Result<AxiomBridgeConnection> {
        val port = SerialPort.getCommPort(portDescriptor)
        port.setComPortParameters(
            baudRate,
            8,
            SerialPort.ONE_STOP_BIT,
            SerialPort.NO_PARITY,
        )
        // port.setFlowControl(SerialPort.FLOW_CONTROL_CTS_ENABLED or SerialPort.FLOW_CONTROL_RTS_ENABLED)
        port.openPort().also { success ->
            if (!success) {
                return Result.failure(RuntimeException("Could not connect to axiom bridge at port $portDescriptor!"))
            }
        }

        return Result.success(AxiomBridgeConnection(architecture, port))
    }
}
