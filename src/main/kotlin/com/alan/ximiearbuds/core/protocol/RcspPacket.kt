package com.alan.ximiearbuds.core.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * Xiaomi RCSP Packet Framing Implementation.
 * Reverse-engineered from com.mi.earphone (f7.f and BasePacket).
 *
 * Wire format:
 * [0..2]   Start bytes: 0xFE, 0xDC, 0xBA
 * [3]      Control byte:
 *            bit 7: Type (1 = Command, 0 = Response)
 *            bit 6: HasResponse (1 = Has response)
 *            bits 0..2: TargetApp (4 = Earphone App, 0 = Default)
 * [4]      OpCode (e.g. 0x02 GetTargetInfo, 0xF2 SetDeviceConfig, 0xF3 GetDeviceConfig, 0xF4 NotifyDeviceConfig)
 * [5..6]   ParamLen (UInt16 Big Endian)
 * [7..N]   Payload:
 *            If Command: [0] opCodeSn, [1..] paramData
 *            If Response: [0] status (0 = success), [1] opCodeSn, [2..] paramData
 * [N+1]    End byte: 0xEF
 */
data class RcspPacket(
    val type: Int = TYPE_COMMAND,
    val hasResponse: Int = FLAG_HAVE_RESPONSE,
    val targetApp: Int = TARGET_APP_EARPHONE,
    val opCode: Int,
    val opCodeSn: Int = nextSequenceNumber(),
    val status: Int = 0,
    val payload: ByteArray = ByteArray(0)
) {
    companion object {
        const val START_BYTE_0 = 0xFE.toByte()
        const val START_BYTE_1 = 0xDC.toByte()
        const val START_BYTE_2 = 0xBA.toByte()
        const val END_BYTE = 0xEF.toByte()

        const val TYPE_COMMAND = 1
        const val TYPE_RESPONSE = 0

        const val FLAG_HAVE_RESPONSE = 1
        const val FLAG_NO_RESPONSE = 0

        const val TARGET_APP_DEFAULT = 0
        const val TARGET_APP_EARPHONE = 4

        // Standard RCSP OpCodes from com.xiaomi.aivsbluetoothsdk.constant.Command
        const val CMD_GET_TARGET_INFO = 2
        const val CMD_SET_TARGET_INFO = 35
        const val CMD_GET_DEVICE_RUN_INFO = 9
        const val CMD_DISCONNECT_CLASSIC_BT = 5
        const val CMD_REBOOT_DEVICE = 28
        const val CMD_FIND_DEVICE = 66
        const val CMD_SET_DEVICE_CONFIG = 242 // 0xF2
        const val CMD_GET_DEVICE_CONFIG = 243 // 0xF3
        const val CMD_NOTIFY_DEVICE_CONFIG = 244 // 0xF4
        const val CMD_SETTINGS_MTU = 30

        private val sequenceCounter = AtomicInteger(1)

        fun nextSequenceNumber(): Int {
            return sequenceCounter.getAndUpdate { if (it >= 255) 1 else it + 1 }
        }

        fun parse(data: ByteArray): RcspPacket? {
            val packets = parseStream(data)
            return packets.firstOrNull()
        }

        fun parseStream(data: ByteArray): List<RcspPacket> {
            val packets = mutableListOf<RcspPacket>()
            if (data.size < 8) return packets

            var offset = 0
            while (offset <= data.size - 8) {
                // Find start sequence 0xFE, 0xDC, 0xBA
                if (data[offset] != START_BYTE_0 ||
                    data[offset + 1] != START_BYTE_1 ||
                    data[offset + 2] != START_BYTE_2
                ) {
                    offset++
                    continue
                }

                if (offset + 7 > data.size) break

                val ctrl = data[offset + 3].toInt() and 0xFF
                val opCode = data[offset + 4].toInt() and 0xFF
                val paramLen = ((data[offset + 5].toInt() and 0xFF) shl 8) or (data[offset + 6].toInt() and 0xFF)

                val packetTotalLen = 3 + 1 + 1 + 2 + paramLen + 1 // Start (3) + Ctrl (1) + OpCode (1) + Len (2) + paramLen + End (1)
                if (offset + packetTotalLen > data.size) {
                    // Incomplete packet in stream
                    break
                }

                val endByte = data[offset + packetTotalLen - 1]
                if (endByte != END_BYTE) {
                    offset++
                    continue
                }

                val isCommand = (ctrl and 0x80) != 0
                val hasResp = if ((ctrl and 0x40) != 0) 1 else 0
                val app = ctrl and 0x07

                val type = if (isCommand) TYPE_COMMAND else TYPE_RESPONSE
                val payloadStart = offset + 7

                var status = 0
                var sn = 0
                val actualPayload: ByteArray

                if (isCommand) {
                    sn = data[payloadStart].toInt() and 0xFF
                    val dataLen = paramLen - 1
                    actualPayload = if (dataLen > 0) {
                        data.copyOfRange(payloadStart + 1, payloadStart + 1 + dataLen)
                    } else {
                        ByteArray(0)
                    }
                } else {
                    status = data[payloadStart].toInt() and 0xFF
                    sn = if (paramLen > 1) data[payloadStart + 1].toInt() and 0xFF else 0
                    val dataLen = paramLen - 2
                    actualPayload = if (dataLen > 0) {
                        data.copyOfRange(payloadStart + 2, payloadStart + 2 + dataLen)
                    } else {
                        ByteArray(0)
                    }
                }

                packets.add(
                    RcspPacket(
                        type = type,
                        hasResponse = hasResp,
                        targetApp = app,
                        opCode = opCode,
                        opCodeSn = sn,
                        status = status,
                        payload = actualPayload
                    )
                )

                offset += packetTotalLen
            }

            return packets
        }
    }

    fun toByteArray(): ByteArray {
        val isCommand = (type == TYPE_COMMAND)
        val paramLen = if (isCommand) payload.size + 1 else payload.size + 2
        val totalLength = 8 + paramLen

        val buffer = ByteBuffer.allocate(totalLength)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // 1. Start sequence (3 bytes)
        buffer.put(START_BYTE_0)
        buffer.put(START_BYTE_1)
        buffer.put(START_BYTE_2)

        // 2. Control byte
        var ctrl = 0
        if (isCommand) ctrl = ctrl or 0x80
        if (hasResponse == 1) ctrl = ctrl or 0x40
        ctrl = ctrl or (targetApp and 0x07)
        buffer.put(ctrl.toByte())

        // 3. OpCode
        buffer.put(opCode.toByte())

        // 4. Param length (BigEndian uint16)
        buffer.putShort(paramLen.toShort())

        // 5. Payload
        if (isCommand) {
            buffer.put(opCodeSn.toByte())
            if (payload.isNotEmpty()) {
                buffer.put(payload)
            }
        } else {
            buffer.put(status.toByte())
            buffer.put(opCodeSn.toByte())
            if (payload.isNotEmpty()) {
                buffer.put(payload)
            }
        }

        // 6. End byte
        buffer.put(END_BYTE)

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RcspPacket
        return type == other.type &&
                hasResponse == other.hasResponse &&
                targetApp == other.targetApp &&
                opCode == other.opCode &&
                opCodeSn == other.opCodeSn &&
                status == other.status &&
                payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + hasResponse
        result = 31 * result + targetApp
        result = 31 * result + opCode
        result = 31 * result + opCodeSn
        result = 31 * result + status
        result = 31 * result + payload.contentHashCode()
        return result
    }

    override fun toString(): String {
        val hexPayload = payload.joinToString(" ") { "%02X".format(it) }
        return "RcspPacket(type=${if (type == 1) "CMD" else "RESP"}, op=0x%02X, sn=$opCodeSn, status=$status, len=${payload.size}, payload=[$hexPayload])"
            .format(opCode)
    }
}
