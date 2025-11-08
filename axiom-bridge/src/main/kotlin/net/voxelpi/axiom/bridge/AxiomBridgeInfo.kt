package net.voxelpi.axiom.bridge

@JvmRecord
public data class AxiomBridgeInfo(
    val protocolVersion: Int,
    val version: String,
    val gitVersion: String,
)
