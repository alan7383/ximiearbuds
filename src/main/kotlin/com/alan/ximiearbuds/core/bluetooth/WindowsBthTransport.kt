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

    private var scanJob: Job? = null

    override fun getPairedDevices(): List<DiscoveredDevice> {
        val list = mutableListOf<DiscoveredDevice>()
        try {
            // Enumerate paired bluetooth devices via PowerShell Get-PnpDevice
            // Extracts exact 12-char MAC from InstanceId (e.g. BTHENUM\DEV_00BB438BC0F3\...)
            val cmd = arrayOf(
                "powershell", "-NoProfile", "-Command",
                "Get-PnpDevice -Class Bluetooth | Where-Object { \$_.InstanceId -match 'DEV_([0-9A-Fa-f]{12})' } | ForEach-Object { \"\$(\$matches[1])`t\$(\$_.FriendlyName)\" }"
            )
            val process = ProcessBuilder(*cmd).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)

            for (line in output.lines()) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && trimmed.contains("\t")) {
                    val rawMac = trimmed.substringBefore("\t").trim()
                    val name = trimmed.substringAfter("\t").trim()
                    if (rawMac.length == 12 && name.isNotBlank()) {
                        val formattedMac = rawMac.chunked(2).joinToString(":").uppercase()
                        val isXiaomi = name.contains("Buds", ignoreCase = true) ||
                                name.contains("Xiaomi", ignoreCase = true) ||
                                name.contains("Redmi", ignoreCase = true) ||
                                name.contains("POCO", ignoreCase = true) ||
                                name.contains("AirDots", ignoreCase = true) ||
                                name.contains("Earphones", ignoreCase = true) ||
                                name.contains("Earbuds", ignoreCase = true)
                        list.add(
                            DiscoveredDevice(
                                name = name,
                                address = formattedMac,
                                isConnected = false,
                                isXiaomiEarbuds = isXiaomi
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Error querying Windows Bluetooth: ${e.message}")
        }
        return list
    }

    override suspend fun scanDevices(): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        getPairedDevices()
    }

    override fun startScanning(onDevicesFound: (List<DiscoveredDevice>) -> Unit) {
        stopScanning()
        scanJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val devices = getPairedDevices()
                if (devices.isNotEmpty()) {
                    onDevicesFound(devices)
                }
                delay(2000)
            }
        }
    }

    override fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }

    override suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            disconnect()
        }

        _connectionState.value = ConnectionState.CONNECTING

        val ws2 = Ws2_32.INSTANCE
        val macLong = try {
            macToLong(address)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return@withContext false
        }

        // Priority channels matching LinuxRfcommTransport:
        // Channel 24 is the dedicated Xiaomi/MIUI RCSP channel (UUID_SPP_MIUI / 0000fd2d).
        // Channels 21, 3, 8 are fallback models.
        val priorityChannels = listOf(24, 21, 3, 8)
        val extendedChannels = (4..30).filter { it !in priorityChannels && it != 1 && it != 2 }
        val candidateChannels = priorityChannels + extendedChannels

        for (ch in candidateChannels) {
            val sock = ws2.socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM)
            if (sock <= 0) continue

            val addr = SockAddrBth()
            addr.addressFamily = AF_BTH.toShort()
            addr.btAddr = macLong
            addr.port = ch

            val res = ws2.connect(sock, addr, addr.size())
            if (res == 0) {
                println("WindowsBthTransport: Connected successfully on RFCOMM channel $ch")
                socketHandle = sock
                _connectionState.value = ConnectionState.CONNECTED
                _connectedDevice.value = DiscoveredDevice(name = "Xiaomi Earbuds", address = address, isConnected = true)
                startListening()
                return@withContext true
            } else {
                ws2.closesocket(sock)
            }
        }

        // Final fallback: SDP channel 0 query
        val sock = ws2.socket(AF_BTH, SOCK_STREAM, BTHPROTO_RFCOMM)
        if (sock > 0) {
            val addr = SockAddrBth()
            addr.addressFamily = AF_BTH.toShort()
            addr.btAddr = macLong
            addr.port = 0
            addr.serviceClassId = Guid.GUID(SERIAL_PORT_SERVICE_CLASS_UUID)

            val res = ws2.connect(sock, addr, addr.size())
            if (res == 0) {
                println("WindowsBthTransport: Connected successfully via SDP channel 0")
                socketHandle = sock
                _connectionState.value = ConnectionState.CONNECTED
                _connectedDevice.value = DiscoveredDevice(name = "Xiaomi Earbuds", address = address, isConnected = true)
                startListening()
                return@withContext true
            } else {
                ws2.closesocket(sock)
            }
        }

        _connectionState.value = ConnectionState.DISCONNECTED
        return@withContext false
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

                    val (parsedPackets, consumed) = RcspPacket.parseStreamWithConsumed(streamAccumulator.toByteArray())
                    if (parsedPackets.isNotEmpty()) {
                        for (pkt in parsedPackets) {
                            _incomingPackets.emit(pkt)
                        }
                        if (consumed > 0 && consumed <= streamAccumulator.size) {
                            val remaining = streamAccumulator.subList(consumed, streamAccumulator.size).toList()
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
