package com.alan.ximiearbuds.core.device

import com.alan.ximiearbuds.core.bluetooth.ConnectionState
import com.alan.ximiearbuds.core.bluetooth.LinuxRfcommTransport
import com.alan.ximiearbuds.core.crypto.BluetoothAuthEngine
import com.alan.ximiearbuds.core.protocol.ConfigId
import com.alan.ximiearbuds.core.protocol.NoiseMode
import com.alan.ximiearbuds.core.protocol.RcspPacket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Live hardware test only when earbuds are in ear and connected")
class LiveHardwareConnectionTest {

    @Test
    fun `test live authentication and ANC toggle on real hardware`() = runBlocking {
        val address = "00:BB:43:8B:C0:F3"
        val transport = LinuxRfcommTransport()

        println("Connecting to $address via LinuxRfcommTransport...")
        val connected = transport.connect(address)
        if (!connected) {
            println("Device not reachable, skipping live test.")
            return@runBlocking
        }

        try {
            println("Connected to Channel 24! Starting SAFER+ E21 Auth handshake...")
            val randFactor = BluetoothAuthEngine.generateRandomFactor()
            val expectedResult = BluetoothAuthEngine.encrypt(randFactor)

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

            val resp80 = withTimeoutOrNull(3000) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_AUTH_CHECK && it.type == RcspPacket.TYPE_RESPONSE }
            }

            println("Received AuthCheckResponse: status=${resp80?.status} payloadLen=${resp80?.payload?.size}")
            assertTrue(resp80 != null && resp80.status == 0)

            val earbudResult = resp80!!.payload.copyOfRange(1, 17)
            val authOk = BluetoothAuthEngine.verifyResponse(randFactor, earbudResult)
            println("SAFER+ E21 result match: $authOk (Earbud computed: ${earbudResult.joinToString("") { "%02x".format(it) }})")
            assertTrue(authOk)

            // 2. Send OpCode 81 AuthSendCalcResultCmd
            val authResultCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_HAVE_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_AUTH_SEND_CALC_RESULT,
                opCodeSn = 2,
                payload = byteArrayOf(0x01, 0x00) // success
            )
            transport.send(authResultCmd)

            val resp81 = withTimeoutOrNull(2000) {
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_AUTH_SEND_CALC_RESULT && it.type == RcspPacket.TYPE_RESPONSE }
            }
            println("Received AuthSendCalcResultResponse: status=${resp81?.status}")

            // 3. Send OpCode 2 GetTargetInfoCmd
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
                transport.incomingPackets.first { it.opCode == RcspPacket.CMD_GET_TARGET_INFO }
            }
            println("Received GetTargetInfoResponse: status=${resp2?.status} payloadLen=${resp2?.payload?.size}")

            // 4. Test physical ANC switch to ANC Deep!
            // OpCode 242 (CMD_SET_DEVICE_CONFIG, NO_RESPONSE)
            // ConfigId.NOISE_LEVEL_CHOOSE (1): [mode=1 (ANC), level=1 (Deep)]
            // CommonConfig: [configId (0x0001), len (0x0002), 0x01, 0x01]
            val ancDeepPayload = byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x01, 0x01)
            val ancCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                opCodeSn = 4,
                payload = ancDeepPayload
            )
            transport.send(ancCmd)
            println("Successfully sent ANC Deep command to real hardware! Pausing 1.5s...")
            delay(1500)

            // 5. Test physical ANC switch to Transparency!
            val transPayload = byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x02, 0x00)
            val transCmd = RcspPacket(
                type = RcspPacket.TYPE_COMMAND,
                hasResponse = RcspPacket.FLAG_NO_RESPONSE,
                targetApp = RcspPacket.TARGET_APP_EARPHONE,
                opCode = RcspPacket.CMD_SET_DEVICE_CONFIG,
                opCodeSn = 5,
                payload = transPayload
            )
            transport.send(transCmd)
            println("Successfully sent Transparency command to real hardware!")
            delay(1000)

            println("=== ALL LIVE HARDWARE ACTIONS EXECUTED PERFECTLY! ===")
        } finally {
            transport.disconnect()
        }
    }
}
