package com.alan.ximiearbuds.core.bluetooth

import com.alan.ximiearbuds.core.protocol.RcspPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isXiaomiEarbuds: Boolean = true,
    val colorType: Int = 0
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * Common cross-platform transport interface for Bluetooth communication with Xiaomi Earbuds.
 */
interface BluetoothTransport {
    val connectionState: StateFlow<ConnectionState>
    val incomingPackets: SharedFlow<RcspPacket>
    val connectedDevice: StateFlow<DiscoveredDevice?>

    fun getPairedDevices(): List<DiscoveredDevice>
    suspend fun scanDevices(): List<DiscoveredDevice> = getPairedDevices()
    fun startScanning(onDevicesFound: (List<DiscoveredDevice>) -> Unit) {}
    fun stopScanning() {}
    suspend fun connect(address: String): Boolean
    suspend fun disconnect()
    suspend fun send(packet: RcspPacket): Boolean
}

