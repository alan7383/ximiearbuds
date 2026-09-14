package com.alan.ximiearbuds.core.bluetooth

import com.alan.ximiearbuds.core.protocol.RcspPacket
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Linux native RFCOMM Bluetooth client using JNA libc sockets.
 * Uses AF_BLUETOOTH (31), SOCK_STREAM (1), BTPROTO_RFCOMM (3).
 */
class LinuxRfcommTransport(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : BluetoothTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingPackets = MutableSharedFlow<RcspPacket>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<RcspPacket> = _incomingPackets

    private val _connectedDevice = MutableStateFlow<DiscoveredDevice?>(null)
    override val connectedDevice: StateFlow<DiscoveredDevice?> = _connectedDevice

    private var socketFd: Int = -1
    private var readJob: Job? = null

    interface LibC : Library {
        fun socket(domain: Int, type: Int, protocol: Int): Int
        fun connect(sockfd: Int, addr: Structure, addrlen: Int): Int
        fun read(fd: Int, buf: ByteArray, count: Int): Int
        fun write(fd: Int, buf: ByteArray, count: Int): Int
        fun close(fd: Int): Int

        companion object {
            val INSTANCE: LibC = Native.load("c", LibC::class.java)
        }
    }

    @Structure.FieldOrder("rc_family", "rc_bdaddr", "rc_channel")
    open class SockAddrRc : Structure() {
        @JvmField var rc_family: Short = 31 // AF_BLUETOOTH
        @JvmField var rc_bdaddr: ByteArray = ByteArray(6) // MAC in reverse byte order
        @JvmField var rc_channel: Byte = 1 // Channel 1

        class ByReference : SockAddrRc(), Structure.ByReference
    }

    companion object {
        const val AF_BLUETOOTH = 31
        const val SOCK_STREAM = 1
        const val BTPROTO_RFCOMM = 3

        fun parseMac(mac: String): ByteArray {
            val parts = mac.split(":").map { it.toInt(16).toByte() }
            // In Linux sockaddr_rc, bdaddr_t is in reverse (Little Endian) byte order
            return parts.reversed().toByteArray()
        }
    }

    private var scanProcess: Process? = null
    private var scanJob: Job? = null

    private data class DeviceDetails(
        val name: String,
        val isXiaomi: Boolean,
        val colorType: Int
    )

    private fun inspectDevice(mac: String, fallbackName: String): DeviceDetails {
        var resolvedName = fallbackName
        var isXiaomi = fallbackName.contains("Buds", ignoreCase = true) ||
                fallbackName.contains("Xiaomi", ignoreCase = true) ||
                fallbackName.contains("Redmi", ignoreCase = true) ||
                fallbackName.contains("POCO", ignoreCase = true) ||
                fallbackName.contains("AirDots", ignoreCase = true) ||
                fallbackName.contains("Earphones", ignoreCase = true) ||
                fallbackName.contains("Earbuds", ignoreCase = true)
        var detectedColor = 0

        try {
            val proc = ProcessBuilder("bluetoothctl", "info", mac).start()
            val text = proc.inputStream.bufferedReader().readText()
            proc.waitFor(1, java.util.concurrent.TimeUnit.SECONDS)

            val lines = text.lines()
            var inManufacturerValue = false
            val mfgBytes = mutableListOf<Byte>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("Name: ")) {
                    resolvedName = trimmed.removePrefix("Name: ").trim()
                } else if (trimmed.startsWith("Alias: ") && (resolvedName.isBlank() || resolvedName.contains("-"))) {
                    resolvedName = trimmed.removePrefix("Alias: ").trim()
                }

                if (trimmed.contains("0x038f", ignoreCase = true) ||
                    trimmed.contains("audio-headset", ignoreCase = true) ||
                    trimmed.contains("fa349b5f", ignoreCase = true)
                ) {
                    isXiaomi = true
                }

                if (trimmed.startsWith("ManufacturerData.Value:")) {
                    inManufacturerValue = true
                    continue
                }

                if (inManufacturerValue) {
                    if (trimmed.isEmpty() || (!line.startsWith(" ") && !line.startsWith("\t"))) {
                        inManufacturerValue = false
                    } else {
                        val parts = trimmed.split(Regex("\\s+"))
                        for (p in parts) {
                            if (p.length == 2 && p.all { it in "0123456789abcdefABCDEF" }) {
                                try {
                                    mfgBytes.add(p.toInt(16).toByte())
                                } catch (_: Exception) { break }
                            } else {
                                break
                            }
                        }
                    }
                }
            }

            // Parse color from Xiaomi BLE manufacturer data byte 12 (ParseScanData.java: bleScanMessage.setColor((bArr11[12] & 0x78) >> 3))
            if (mfgBytes.size >= 13) {
                val c = (mfgBytes[12].toInt() and 0x78) ushr 3
                if (c > 0) {
                    detectedColor = c
                    com.alan.ximiearbuds.core.device.DevicePreferences.saveDeviceColor(mac, c)
                }
            }
        } catch (_: Exception) {}

        if (detectedColor == 0) {
            detectedColor = com.alan.ximiearbuds.core.device.DevicePreferences.getDeviceColor(mac)
                ?: com.alan.ximiearbuds.core.device.DevicePreferences.getDeviceColor(resolvedName)
                ?: 0
        }

        return DeviceDetails(
            name = resolvedName,
            isXiaomi = isXiaomi,
            colorType = detectedColor
        )
    }

    private fun queryDevices(): List<DiscoveredDevice> {
        val list = mutableListOf<DiscoveredDevice>()
        try {
            val process = ProcessBuilder("bluetoothctl", "devices").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)

            for (line in output.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("Device ")) {
                    val parts = trimmed.split(" ", limit = 3)
                    if (parts.size >= 3) {
                        val mac = parts[1]
                        val rawName = parts[2]
                        val details = inspectDevice(mac, rawName)
                        list.add(
                            DiscoveredDevice(
                                name = details.name,
                                address = mac,
                                isConnected = false,
                                isXiaomiEarbuds = details.isXiaomi,
                                colorType = details.colorType
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Error querying bluetoothctl: ${e.message}")
        }
        return list
    }

    override fun getPairedDevices(): List<DiscoveredDevice> {
        return queryDevices()
    }

    override suspend fun scanDevices(): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        try {
            val proc = ProcessBuilder("timeout", "3", "bluetoothctl", "--agent", "NoInputNoOutput", "scan", "on").start()
            proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {}
        queryDevices()
    }

    override fun startScanning(onDevicesFound: (List<DiscoveredDevice>) -> Unit) {
        stopScanning()
        scanJob = scope.launch(Dispatchers.IO) {
            try {
                scanProcess = ProcessBuilder("bluetoothctl", "--agent", "NoInputNoOutput", "scan", "on").start()
                while (isActive) {
                    val devices = queryDevices()
                    if (devices.isNotEmpty()) {
                        onDevicesFound(devices)
                    }
                    delay(1200)
                }
            } catch (e: Exception) {
                while (isActive) {
                    onDevicesFound(queryDevices())
                    delay(2000)
                }
            }
        }
    }

    override fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        try {
            scanProcess?.destroyForcibly()
        } catch (_: Exception) {}
        scanProcess = null
        try {
            ProcessBuilder("bluetoothctl", "scan", "off").start().waitFor(1, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {}
    }

    override suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            disconnect()
        }

        _connectionState.value = ConnectionState.CONNECTING

        // 1. Ensure BlueZ pairs/trusts the device if in pairing mode
        try {
            val pairScript = "echo -e 'trust $address\\npair $address\\nconnect $address\\nquit\\n' | bluetoothctl --agent NoInputNoOutput"
            val proc = ProcessBuilder("bash", "-c", pairScript).start()
            proc.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {}

        try {
            val libc = LibC.INSTANCE
            val fd = libc.socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)
            var connected = false

            if (fd >= 0) {
                val addr = SockAddrRc()
                addr.rc_family = AF_BLUETOOTH.toShort()
                addr.rc_bdaddr = parseMac(address)
                addr.rc_channel = 1 // Standard RFCOMM Channel 1 for SPP

                val res = libc.connect(fd, addr, addr.size())
                if (res >= 0) {
                    socketFd = fd
                    connected = true
                } else {
                    libc.close(fd)
                    // Try channel 2, 3, 4 if channel 1 rejected
                    for (ch in 2..4) {
                        addr.rc_channel = ch.toByte()
                        val fdRetry = libc.socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)
                        if (fdRetry >= 0) {
                            if (libc.connect(fdRetry, addr, addr.size()) == 0) {
                                socketFd = fdRetry
                                connected = true
                                break
                            } else {
                                libc.close(fdRetry)
                            }
                        }
                    }
                }
            }

            // Check if BlueZ itself reports connected
            val isBluezConnected = try {
                val check = ProcessBuilder("bluetoothctl", "info", address).start()
                val txt = check.inputStream.bufferedReader().readText()
                check.waitFor(1, java.util.concurrent.TimeUnit.SECONDS)
                txt.contains("Connected: yes", ignoreCase = true)
            } catch (_: Exception) { false }

            if (connected || isBluezConnected) {
                _connectionState.value = ConnectionState.CONNECTED
                val details = inspectDevice(address, "Xiaomi Earbuds")
                val matchedDev = queryDevices().find { it.address.equals(address, ignoreCase = true) }
                    ?: DiscoveredDevice(
                        name = details.name,
                        address = address,
                        isConnected = true,
                        isXiaomiEarbuds = details.isXiaomi,
                        colorType = details.colorType
                    )
                _connectedDevice.value = matchedDev

                if (connected) {
                    startListening()
                }
                return@withContext true
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext false
            }
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

            while (isActive && socketFd >= 0) {
                val bytesRead = LibC.INSTANCE.read(socketFd, buffer, buffer.size)
                if (bytesRead > 0) {
                    for (i in 0 until bytesRead) {
                        streamAccumulator.add(buffer[i])
                    }

                    // Parse packets from accumulated stream
                    val parsedPackets = RcspPacket.parseStream(streamAccumulator.toByteArray())
                    if (parsedPackets.isNotEmpty()) {
                        for (pkt in parsedPackets) {
                            _incomingPackets.emit(pkt)
                        }
                        // Clear parsed bytes from buffer by keeping remaining
                        val lastPacket = parsedPackets.last()
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
        if (socketFd >= 0) {
            LibC.INSTANCE.close(socketFd)
            socketFd = -1
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
    }

    override suspend fun send(packet: RcspPacket): Boolean = withContext(Dispatchers.IO) {
        if (socketFd < 0 || _connectionState.value != ConnectionState.CONNECTED) {
            return@withContext false
        }

        val data = packet.toByteArray()
        val written = LibC.INSTANCE.write(socketFd, data, data.size)
        return@withContext written == data.size
    }
}
