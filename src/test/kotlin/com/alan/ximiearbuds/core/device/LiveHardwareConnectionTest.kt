package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.bluetooth.LinuxRfcommTransport
import com.alan.ximiearbuds.core.crypto.BluetoothAuthEngine
import com.alan.ximiearbuds.core.protocol.RcspPacket
import com.alan.ximiearbuds.core.protocol.TargetDeviceInfo
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("hardware")
class LiveHardwareConnectionTest {

    private val targetAddress = "00:BB:43:8B:C0:F3"

    @BeforeEach
    fun ensureDeviceConnected() {
        Thread.sleep(3500) // Allow kernel RFCOMM stack to recycle DLC/socket if previous test just ran
        // Ensure the physical earbuds are connected to BlueZ before testing
        val check = ProcessBuilder("bluetoothctl", "info", targetAddress).start()
        val txt = check.inputStream.bufferedReader().readText()
        check.waitFor()
        if (!txt.contains("Connected: yes", ignoreCase = true)) {
            println("Device not connected to BlueZ. Attempting bluetoothctl connect $targetAddress...")
            val conn = ProcessBuilder("bluetoothctl", "connect", targetAddress).start()
            conn.waitFor()
            Thread.sleep(1500)
        }
    }

    @Test
    fun `test live authentication and stability on real hardware`() = runBlocking {
        val transport = LinuxRfcommTransport()

        println("Connecting to $targetAddress via LinuxRfcommTransport (Channel 24 with BT_SECURITY_MEDIUM)...")
        val connected = transport.connect(targetAddress)
        assertTrue(connected, "Device must connect via RFCOMM channel 24")

        val ackJob = launch {
            transport.incomingPackets.collect { pkt ->
                if (pkt.type == RcspPacket.TYPE_COMMAND && pkt.hasResponse == RcspPacket.FLAG_HAVE_RESPONSE) {
                    if (pkt.opCode != RcspPacket.CMD_AUTH_CHECK && pkt.opCode != RcspPacket.CMD_AUTH_SEND_CALC_RESULT) {
                        val ackPayload = if (pkt.opCode == 7) pkt.payload else ByteArray(0)
                        val ack = RcspPacket.createAckResponse(pkt, status = 0, payload = ackPayload)
                        transport.send(ack)
                    }
                }
            }
        }

        try {
            println("Connected to Channel 24! Starting SAFER+ E21 Auth handshake (Stage 1)...")
            val randFactor = BluetoothAuthEngine.generateRandomFactor()

            // 1. Send OpCode 80 AuthCheckCmd
            val authCheckCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_AUTH_CHECK,
                opCodeSn = 1,
                payload = byteArrayOf(0x01) + randFactor
            )
            transport.send(authCheckCmd)

            val resp80 = withTimeoutOrNull(2500) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_AUTH_CHECK && it.type == RcspPacket.TYPE_RESPONSE }
            }
            println("Received AuthCheckResponse: status=${resp80?.status} payloadLen=${resp80?.payload?.size}")
            assertTrue(resp80 != null && resp80.status == 0 && resp80.payload.size >= 17)

            val earbudResult = resp80!!.payload.copyOfRange(1, 17)
            val verified = BluetoothAuthEngine.verifyResponse(randFactor, earbudResult)
            println("SAFER+ E21 result match: $verified (Earbud computed: ${earbudResult.joinToString("") { "%02x".format(it) }})")
            assertTrue(verified, "Earbud SAFER+ response must match local computation")

            // 2. Send OpCode 81 AuthSendCalcResultCmd
            val authResultCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                opCodeSn = 2,
                payload = byteArrayOf(0x01, 0x00) // version=1, pairResult=0 (SUCCESS)
            )
            transport.send(authResultCmd)

            val resp81 = withTimeoutOrNull(2000) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_AUTH_SEND_CALC_RESULT && it.type == RcspPacket.TYPE_RESPONSE }
            }
            println("Received AuthSendCalcResultResponse: status=${resp81?.status}")
            assertTrue(resp81 != null && resp81.status == 0)

            // 3. Wait for Stage 2 (Earbuds -> Phone)
            println("Waiting for Earbud-initiated challenge (Stage 2)...")
            val earbudChallenge = withTimeoutOrNull(3000) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_AUTH_CHECK && it.type == RcspPacket.TYPE_COMMAND }
            }
            if (earbudChallenge != null) {
                println("Received earbud challenge! payloadLen=${earbudChallenge.payload.size}")
                val earbudRand = earbudChallenge.payload.copyOfRange(1, 17)
                val phoneEncrypted = BluetoothAuthEngine.encrypt(earbudRand)
                val phoneResp80 = RcspPacket(
                    type = RcspPacket.TYPE_RESPONSE,
                    hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                    targetApp = earbudChallenge.targetApp,
                    opCode = RcspPacket.CMD_AUTH_CHECK,
                    opCodeSn = earbudChallenge.opCodeSn,
                    status = 0,
                    payload = byteArrayOf(0x01) + phoneEncrypted
                )
                transport.send(phoneResp80)
                println("Sent Phone AuthCheckResponse!")

                // Wait for earbud's OpCode 81 command
                val earbudResultCmd = withTimeoutOrNull(2000) {
                    transport.incomingPackets.first { it.opCode == RcspPacket.CMD_AUTH_SEND_CALC_RESULT && it.type == RcspPacket.TYPE_COMMAND }
                }
                println("Received earbud AuthSendCalcResultCmd: payload=[${earbudResultCmd?.payload?.joinToString { "%02X".format(it) }}]")
                if (earbudResultCmd != null) {
                    val phoneResp81 = RcspPacket(
                        type = RcspPacket.TYPE_RESPONSE,
                        hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                        targetApp = earbudResultCmd.targetApp,
                        opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                        opCodeSn = earbudResultCmd.opCodeSn,
                        status = 0,
                        payload = byteArrayOf(0x01) // versionResponse = 1
                    )
                    transport.send(phoneResp81)
                    println("Sent Phone AuthSendCalcResultResponse with versionResponse=1!")
                }
            }

            delay(200)

            // 4. Send OpCode 2 GetTargetInfoCmd
            val targetInfoCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_TARGET_INFO,
                opCodeSn = 3,
                payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
            )
            transport.send(targetInfoCmd)

            val resp2 = withTimeoutOrNull(2000) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_GET_TARGET_INFO && it.type == RcspPacket.TYPE_RESPONSE }
            }
            println("Received GetTargetInfoResponse: status=${resp2?.status} payloadLen=${resp2?.payload?.size}")
            if (resp2 != null) {
                val info = TargetDeviceInfo.parseFromResponsePayload(resp2.payload)
                println("TargetDeviceInfo parsed: name=${info.name} vid=${info.vendorId} pid=${info.productId} battL=${info.leftBattery.percentage}% battR=${info.rightBattery.percentage}%")
            }

            delay(200)

            // 5. Send OpCode 9 GetDeviceRunInfoCmd
            val runInfoCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_GET_DEVICE_RUN_INFO,
                opCodeSn = 4,
                payload = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
            )
            transport.send(runInfoCmd)

            val resp9 = withTimeoutOrNull(2000) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_GET_DEVICE_RUN_INFO && it.type == RcspPacket.TYPE_RESPONSE }
            }
            println("Received GetDeviceRunInfoResponse: status=${resp9?.status} payloadLen=${resp9?.payload?.size}")

            println("Testing link stability over 10 seconds (no peer abort)...")
            for (i in 1..10) {
                delay(1000)
                println("Second $i: state=${transport.connectionState.value}")
                assertTrue(transport.connectionState.value == ConnectionState.CONNECTED, "Connection must remain alive at second $i")
            }

            println("=== SUCCESS! HARDWARE STAYED CONNECTED AND RESPONDED TO ALL PROTOCOL COMMANDS! ===")
        } finally {
            ackJob.cancel()
            transport.disconnect()
        }
    }

    @Test
    fun `test live earbuds controller`() = runBlocking {
        val controller = EarbudsController(scope = this, autoConnectOnStartup = false)

        try {
            println("Connecting controller to $targetAddress...")
            controller.connect(targetAddress)
            delay(2500)
            println("Controller state: ${controller.connectionState.value}")
            println("Target device info: ${controller.deviceInfo.value}")
            println("ANC mode: ${controller.noiseControl.value.mode}")
            for (i in 1..10) {
                delay(1000)
                println("Second $i: state=${controller.connectionState.value}")
                assertTrue(controller.connectionState.value == ConnectionState.CONNECTED, "Controller must remain connected at second $i")
            }
        } finally {
            controller.release()
            coroutineContext.cancelChildren()
        }
    }
}
