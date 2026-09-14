package com.alan.ximiearbuds.core.bluetooth

import com.alan.ximiearbuds.core.protocol.RcspPacket
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Guid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Windows native Bluetooth RFCOMM client using Winsock 2 (ws2_32.dll) via JNA.
 * Uses AF_BTH (32), BTHPROTO_RFCOMM (3), SOCKADDR_BTH.
 */
class WindowsBthTransport(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : BluetoothTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingPackets = MutableSharedFlow<RcspPacket>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<RcspPacket> = _incomingPackets

    private val _connectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    override val connectedDevice: StateFlow<DiscoveredDevice?> = _connectedDevice

    private var socketHandle: Long = -1
    private var readJob: Job? = null

    interface Ws2_32 : Library {
        fun WSAStartup(wVersionRequired: Short, lpWSAData: Structure): Int
        fun WSACleanup(): Int
        fun socket(af: Int, type: Int, protocol: Int): Long
        fun connect(s: Long, name: Structure, namelen: Int): Int
        fun recv(s: Long, buf: ByteArray, len: Int, flags: Int): Int
        fun send(s: Long, buf: ByteArray, len: Int, flags: Int): Int
        fun closesocket(s: Long): Int

        companion object {
            val INSTANCE: Ws2_32 by lazy {
                Native.load("ws2_32", Ws2_32::class.java)
            }
        }
    }

    @Structure.FieldOrder("wVersion", "wHighVersion", "szDescription", "szSystemStatus", "iMaxSockets", "iMaxUdpDg", "lpVendorInfo")
    open class WSAData : Structure() {
        @JvmField var wVersion: Short = 0
        @JvmField var wHighVersion: Short = 0
        @JvmField var szDescription: ByteArray = ByteArray(257)
        @JvmField var szSystemStatus: ByteArray = ByteArray(129)
        @JvmField var iMaxSockets: Short = 0
        @JvmField var iMaxUdpDg: Short = 0
        @JvmField var lpVendorInfo: Long = 0
    }

    @Structure.FieldOrder("addressFamily", "btAddr", "serviceClassId", "port")
    open class SockAddrBth : Structure() {
        @JvmField var addressFamily: Short = 32 // AF_BTH
        @JvmField var btAddr: Long = 0 // 64-bit int Bluetooth Address
        @JvmField var serviceClassId: Guid.GUID = Guid.GUID(SERIAL_PORT_SERVICE_CLASS_UUID)
        @JvmField var port: Int = 0 // 0 for SDP query or RFCOMM channel
    }

    companion object {
        const val AF_BTH = 32
        const val SOCK_STREAM = 1
        const val BTHPROTO_RFCOMM = 3

        // SerialPortServiceClass_UUID: 00001101-0000-1000-8000-00805F9B34FB
        const val SERIAL_PORT_SERVICE_CLASS_UUID = "00001101-0000-1000-8000-00805F9B34FB"

        fun macToLong(mac: String): Long {
            val clean = mac.replace(":", "").replace("-", "")
            return clean.toLong(16)
        }
    }

    init {
        try {
            val wsaData = WSAData()
            Ws2_32.INSTANCE.WSAStartup(0x0202.toShort(), wsaData)
        } catch (_: Throwable) {}
    }

    override fun getPairedDevices(): List<DiscoveredDevice> {
        val list = mutableListOf<DiscoveredDevice>()
        try {
            // Enumerate paired bluetooth devices via PowerShell Get-PnpDevice
            val cmd = arrayOf(
                "powershell", "-NoProfile", "-Command",
                "Get-PnpDevice -Class Bluetooth | Where-Object { \$_.FriendlyName -ne \$null -and \$_.Status -eq 'OK' } | Select-Object -Property FriendlyName, InstanceId"
            )
            val process = ProcessBuilder(*cmd).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            for (line in output.lines()) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("FriendlyName") && !trimmed.startsWith("----")) {
                    val name = trimmed.split("  ").firstOrNull()?.trim() ?: continue
                    val isXiaomi = name.contains("Buds", ignoreCase = true) ||
                            name.contains("Xiaomi", ignoreCase = true) ||
                            name.contains("Redmi", ignoreCase = true) ||
                            name.contains("POCO", ignoreCase = true)
                    list.add(DiscoveredDevice(name = name, address = name, isConnected = false, isXiaomiEarbuds = isXiaomi))
                }
            }
        } catch (e: Exception) {
            println("Error querying Windows Bluetooth: ${e.message}")
        }
        return list
    }

    override suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            disconnect()
        }

        _connectionState.value = ConnectionState.CONNECTING

        try {
            val ws2 = Ws2_32.INSTANCE
            val sock = ws2.socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM)
            if (sock <= 0) {
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext false
            }

            val addr = SockAddrBth()
            addr.addressFamily = AF_BTH.toShort()
            addr.btAddr = macToLong(address)
            addr.port = 1 // RFCOMM channel 1

            val res = ws2.connect(sock, addr, addr.size())
            if (res != 0) {
                // Try channel 0 (SDP lookup)
                addr.port = 0
                val retryRes = ws2.connect(sock, addr, addr.size())
                if (retryRes != 0) {
                    ws2.closesocket(sock)
                    _connectionState.value = ConnectionState.DISCONNECTED
                    return@withContext false
                }
            }

            socketHandle = sock
            _connectionState.value = ConnectionState.CONNECTED
            _connectedDevice.value = DiscoveredDevice(name = "Xiaomi Earbuds", address = address, isConnected = true)

            startListening()
            return@withContext true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return@withContext false
        }
    }

    private fun startListening() {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            val streamAccumulator = mutableListOf<Byte>()

            while (isActive && socketHandle > 0) {
                val bytesRead = Ws2_32.INSTANCE.recv(socketHandle, buffer, buffer.size, 0)
                if (bytesRead > 0) {
                    for (i in 0 until bytesRead) {
                        streamAccumulator.add(buffer[i])
                    }

                    val parsedPackets = RcspPacket.parseStream(streamAccumulator.toByteArray())
                    if (parsedPackets.isNotEmpty()) {
                        for (pkt in parsedPackets) {
                            _incomingPackets.emit(pkt)
                        }
                        val lastEndIdx = streamAccumulator.lastIndexOf(RcspPacket.END_BYTE)
                        if (lastEndIdx >= 0 && lastEndIdx + 1 <= streamAccumulator.size) {
                            val remaining = streamAccumulator.subList(lastEndIdx + 1, streamAccumulator.size).toList()
                            streamAccumulator.clear()
                            streamAccumulator.addAll(remaining)
                        }
                    }
                } else if (bytesRead < 0) {
                    break
                }
                delay(10)
            }

            disconnect()
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        readJob?.cancel()
        readJob = null
        if (socketHandle > 0) {
            Ws2_32.INSTANCE.closesocket(socketHandle)
            socketHandle = -1
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
    }

    override suspend fun send(packet: RcspPacket): Boolean = withContext(Dispatchers.IO) {
        if (socketHandle <= 0 || _connectionState.value != ConnectionState.CONNECTED) {
            return@withContext false
        }

        val data = packet.toByteArray()
        val sent = Ws2_32.INSTANCE.send(socketHandle, data, data.size, 0)
        return@withContext sent == data.size
    }
}
