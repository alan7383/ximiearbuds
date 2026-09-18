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
    private var connectedChannel: Int = -1
    private var readJob: Job? = null
    private var rawDumpCounter: Int = 0

    interface LibC : Library {
        fun socket(domain: Int, type: Int, protocol: Int): Int
        fun connect(sockfd: Int, addr: Structure, addrlen: Int): Int
        fun setsockopt(sockfd: Int, level: Int, optname: Int, optval: ByteArray, optlen: Int): Int
        fun read(fd: Int, buf: ByteArray, count: Int): Int
        fun write(fd: Int, buf: ByteArray, count: Int): Int
        fun close(fd: Int): Int
        fun shutdown(fd: Int, how: Int): Int
        fun poll(fds: PollFd.ByReference, nfds: Int, timeoutMs: Int): Int

        companion object {
            val INSTANCE: LibC = Native.load("c", LibC::class.java)
        }
    }

    @Structure.FieldOrder("fd", "events", "revents")
    open class PollFd : Structure() {
        @JvmField var fd: Int = -1
        @JvmField var events: Short = 1 // POLLIN
        @JvmField var revents: Short = 0

        class ByReference : PollFd(), Structure.ByReference
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
        const val SOL_BLUETOOTH = 274
        const val BT_SECURITY = 4
        const val BT_SECURITY_MEDIUM: Byte = 2
        const val POLLIN: Short = 0x0001

        fun parseMac(mac: String): ByteArray {
            val parts = mac.split(":").map { it.toInt(16).toByte() }
            // In Linux sockaddr_rc, bdaddr_t is in reverse (Little Endian) byte order
            return parts.reversed().toByteArray()
        }
    }

    /**
     * Valide qu'un socket RFCOMM parle vraiment RCSP avant de l'accepter.
     * Envoie AuthCheck (80) puis GetTargetInfo (2), attend un paquet valide
     * (FE DC BA ... EF) via poll() avec timeout. Retourne true si l'écouteur répond.
     * Bloquant, à appeler uniquement depuis Dispatchers.IO.
     */
    private fun probeRcsp(fd: Int): Boolean {
        val libc = LibC.INSTANCE
        try {
            // Sur les canaux secondaires (ex: 21, 3), sonde uniquement avec CMD_GET_TARGET_INFO (OpCode 2).
            // Ne JAMAIS envoyer CMD_AUTH_CHECK (80) en probe car cela initialise une session d'authentification
            // SAFER+ E21 incomplète que l'écouteur attend de finaliser, corrompant l'automate du firmware.
            val probes = listOf(
                RcspPacket(
                    type = RcspPacket.TYPE_COMMAND,
                    hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                    targetApp = RcspPacket.TARGET_APP_EARPHONE,
                    opCode = RcspPacket.CMD_GET_TARGET_INFO,
                    payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                ).toByteArray()
            )
            val acc = mutableListOf<Byte>()
            for (probe in probes) {
                libc.write(fd, probe, probe.size)
                val deadline = System.currentTimeMillis() + 1100
                while (System.currentTimeMillis() < deadline) {
                    val pfd = PollFd.ByReference()
                    pfd.fd = fd
                    pfd.events = POLLIN
                    val pr = libc.poll(pfd, 1, 200)
                    if (pr > 0 && (pfd.revents.toInt() and POLLIN.toInt()) != 0) {
                        val buf = ByteArray(2048)
                        val n = libc.read(fd, buf, buf.size)
                        if (n > 0) {
                            for (i in 0 until n) acc.add(buf[i])
                            val packets = RcspPacket.parseStream(acc.toByteArray())
                            if (packets.isNotEmpty()) return true
                        } else if (n <= 0) {
                            return false // peer fermé pendant le probe
                        }
                    } else if (pr < 0) {
                        return false
                    }
                }
            }
            return false
        } catch (_: Exception) {
            return false
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

        // 1. Ensure BlueZ is connected to the device if not already connected.
        // NEVER run 'pair' on an already paired device, as re-pairing aborts the active ACL link!
        try {
            val checkProc = ProcessBuilder("bluetoothctl", "info", address).start()
            val infoText = checkProc.inputStream.bufferedReader().readText()
            checkProc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            if (!infoText.contains("Connected: yes", ignoreCase = true)) {
                println("LinuxRfcommTransport: Device not connected in BlueZ. Running bluetoothctl connect $address...")
                val connProc = ProcessBuilder("bluetoothctl", "connect", address).start()
                connProc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                delay(1000)
            }
        } catch (_: Exception) {}

        try {
            val libc = LibC.INSTANCE
            var connected = false
            // Stratégie canaux prod (tous modèles) :
            // - Priorité aux canaux connus Xiaomi (rapide) : 24 miwear (FD2D, Buds modernes),
            //   21 Airoha_APP, 3 mitaw, 8 BTNOTIFYR6.
            // - Puis balayage étendu 4..30 (hors 1 HFP / 2 HSP = téléphonie audio) pour les
            //   modèles dont le canal RCSP diffère. Seuls les canaux acceptant le TCP sont
            //   probés (les refusés échouent en ms), donc le scan étendu reste rapide.
            // - Chaque canal accepté est VALIDÉ par handshake RCSP (probeRcsp), jamais
            //   accepté sur le seul TCP connect (le 3/mitaw accepte mais reste muet).
            val priorityChannels = listOf(24, 21, 3, 8)
            val extendedChannels = (4..30).filter { it !in priorityChannels }
            val candidateChannels = priorityChannels + extendedChannels

            for (ch in candidateChannels) {
                val fd = libc.socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)
                if (fd >= 0) {
                    val sec = byteArrayOf(BT_SECURITY_MEDIUM, 0x00)
                    val optRes = libc.setsockopt(fd, SOL_BLUETOOTH, BT_SECURITY, sec, sec.size)
                    if (optRes < 0) {
                        val err = Native.getLastError()
                        println("LinuxRfcommTransport: setsockopt BT_SECURITY failed (res=$optRes errno=$err)")
                    } else {
                        println("LinuxRfcommTransport: setsockopt BT_SECURITY_MEDIUM applied successfully!")
                    }

                    val addr = SockAddrRc()
                    addr.rc_family = AF_BLUETOOTH.toShort()
                    addr.rc_bdaddr = parseMac(address)
                    addr.rc_channel = ch.toByte()

                    var currentFd = fd
                    var res = libc.connect(currentFd, addr, addr.size())
                    if (res < 0 && Native.getLastError() == 16 && ch == 24) {
                        // Channel 24 was recently closed and is recycling in kernel/BlueZ, retry with fresh socket
                        for (retry in 1..4) {
                            println("LinuxRfcommTransport: Channel 24 busy (errno=16 EBUSY), waiting 800ms ($retry/4)...")
                            libc.close(currentFd)
                            delay(800)
                            currentFd = libc.socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)
                            if (currentFd < 0) break
                            libc.setsockopt(currentFd, SOL_BLUETOOTH, BT_SECURITY, sec, sec.size)
                            res = libc.connect(currentFd, addr, addr.size())
                            if (res >= 0) break
                        }
                    }
                    if (res >= 0) {
                        if (ch == 24) {
                            println("LinuxRfcommTransport: Successfully connected to dedicated Xiaomi RCSP channel 24 (UUID_SPP_MIUI)")
                            socketFd = currentFd
                            connected = true
                            connectedChannel = ch
                            break
                        }
                        println("LinuxRfcommTransport: TCP connected on channel $ch, probing RCSP...")
                        if (probeRcsp(currentFd)) {
                            println("LinuxRfcommTransport: Successfully connected to RCSP channel $ch (validated)")
                            socketFd = currentFd
                            connected = true
                            connectedChannel = ch
                            break
                        } else {
                            println("LinuxRfcommTransport: Channel $ch mute (no RCSP response), trying next.")
                            libc.close(currentFd)
                        }
                    } else {
                        val errno = Native.getLastError()
                        println("LinuxRfcommTransport: Channel $ch refused (errno=$errno).")
                        libc.close(currentFd)
                    }
                }
            }

            if (connected) {
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
                startListening()
                return@withContext true
            } else {
                println("LinuxRfcommTransport: Unable to establish RFCOMM connection to $address on channels $candidateChannels.")
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectedDevice.value = null
                return@withContext false
            }
        } catch (e: Exception) {
            println("LinuxRfcommTransport: Connection exception: ${e.message}")
            _connectionState.value = ConnectionState.DISCONNECTED
            _connectedDevice.value = null
            return@withContext false
        }
    }

    private fun startListening() {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            val streamAccumulator = mutableListOf<Byte>()

            while (isActive && socketFd >= 0) {
                val pfd = PollFd.ByReference()
                pfd.fd = socketFd
                pfd.events = POLLIN
                val pr = LibC.INSTANCE.poll(pfd, 1, 250)
                if (!isActive || socketFd < 0) break
                if (pr < 0) {
                    val errno = Native.getLastError()
                    if (errno == 4 /* EINTR */) continue
                    break
                }
                if (pr == 0) continue // 250ms timeout elapsed, re-check isActive and socketFd

                if ((pfd.revents.toInt() and POLLIN.toInt()) != 0) {
                    val bytesRead = LibC.INSTANCE.read(socketFd, buffer, buffer.size)
                    if (bytesRead > 0) {
                        for (i in 0 until bytesRead) {
                            streamAccumulator.add(buffer[i])
                        }

                        // Parse packets from accumulated stream
                        val (parsedPackets, consumed) = RcspPacket.parseStreamWithConsumed(streamAccumulator.toByteArray())
                        if (parsedPackets.isNotEmpty()) {
                            for (pkt in parsedPackets) {
                                val hex = pkt.payload.joinToString(" ") { "%02X".format(it) }
                                println("LinuxRfcommTransport: Incoming RCSP packet opCode=${pkt.opCode} type=${pkt.type} hasResp=${pkt.hasResponse} sn=${pkt.opCodeSn} status=${pkt.status} payloadLen=${pkt.payload.size} payload=[$hex]")
                                _incomingPackets.emit(pkt)
                            }
                            if (consumed > 0 && consumed <= streamAccumulator.size) {
                                val remaining = streamAccumulator.subList(consumed, streamAccumulator.size).toList()
                                streamAccumulator.clear()
                                streamAccumulator.addAll(remaining)
                            }
                        }
                    } else if (bytesRead < 0) {
                        val errno = Native.getLastError()
                        println("LinuxRfcommTransport: RFCOMM read error on channel $connectedChannel (bytesRead=$bytesRead errno=$errno), peer closed.")
                        break
                    } else if (bytesRead == 0) {
                        println("LinuxRfcommTransport: RFCOMM EOF on channel $connectedChannel (peer closed).")
                        break
                    }
                }
            }

            if (socketFd >= 0) {
                disconnect()
            }
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        val fd = socketFd
        socketFd = -1
        readJob?.cancel()
        readJob = null
        if (fd >= 0) {
            LibC.INSTANCE.shutdown(fd, 2)
            LibC.INSTANCE.close(fd)
        }
        connectedChannel = -1
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
    }

    override suspend fun send(packet: RcspPacket): Boolean = withContext(Dispatchers.IO) {
        if (socketFd < 0 || _connectionState.value != ConnectionState.CONNECTED) {
            println("LinuxRfcommTransport: Cannot send packet opCode=${packet.opCode} (socketFd=$socketFd, state=${_connectionState.value})")
            return@withContext false
        }

        val data = packet.toByteArray()
        val written = LibC.INSTANCE.write(socketFd, data, data.size)
        val hex = packet.payload.joinToString(" ") { "%02X".format(it) }
        println("LinuxRfcommTransport: Sent packet opCode=${packet.opCode} type=${packet.type} sn=${packet.opCodeSn} size=${data.size} written=$written payload=[$hex]")
        return@withContext written == data.size
    }
}
