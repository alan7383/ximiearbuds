package com.alan.ximiearbuds.core.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Xiaomi RCSP CommonConfig (TLV).
 * Reverse-engineered from com.xiaomi.aivsbluetoothsdk.protocol.rcsp.data.CommonConfig.
 *
 * Wire format:
 * Regular:
 *   [0]    Length (1 byte) = value.length + 2
 *   [1..2] Config Type ID (2 bytes, Big Endian)
 *   [3..N] Config Value (Length - 2 bytes)
 *
 * Special Type 52 (Big Data):
 *   [0]    0xFF
 *   [1..2] Type (0x0034 = 52)
 *   [3..4] Length (2 bytes, Big Endian)
 *   [5..N] Value
 */
data class CommonConfig(
    val type: Int,
    val value: ByteArray
) {
    companion object {
        const val SPEC_TYPE = 52

        fun parseList(data: ByteArray): List<CommonConfig> {
            val list = mutableListOf<CommonConfig>()
            var offset = 0
            val totalLen = data.size

            while (offset < totalLen) {
                val lenByte = data[offset].toInt() and 0xFF

                if (lenByte != 0xFF) {
                    // Regular TLV
                    val tlvLen = lenByte + 1
                    if (offset + tlvLen > totalLen) break

                    if (lenByte >= 2) {
                        val configType = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset + 2].toInt() and 0xFF)
                        val valLen = lenByte - 2
                        val valBytes = if (valLen > 0) {
                            data.copyOfRange(offset + 3, offset + 3 + valLen)
                        } else {
                            ByteArray(0)
                        }
                        list.add(CommonConfig(type = configType, value = valBytes))
                    }
                    offset += tlvLen
                } else {
                    // Special type 52
                    if (offset + 5 > totalLen) break
                    val configType = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset + 2].toInt() and 0xFF)
                    val valLen = ((data[offset + 3].toInt() and 0xFF) shl 8) or (data[offset + 4].toInt() and 0xFF)
                    val tlvLen = 5 + valLen
                    if (offset + tlvLen > totalLen) break

                    val valBytes = data.copyOfRange(offset + 5, offset + 5 + valLen)
                    list.add(CommonConfig(type = configType, value = valBytes))
                    offset += tlvLen
                }
            }

            return list
        }

        fun toByteArray(configs: List<CommonConfig>): ByteArray {
            var totalSize = 0
            for (cfg in configs) {
                totalSize += if (cfg.type == SPEC_TYPE) {
                    5 + cfg.value.size
                } else {
                    1 + 2 + cfg.value.size
                }
            }

            val buffer = ByteBuffer.allocate(totalSize)
            buffer.order(ByteOrder.BIG_ENDIAN)

            for (cfg in configs) {
                if (cfg.type == SPEC_TYPE) {
                    buffer.put(0xFF.toByte())
                    buffer.putShort(cfg.type.toShort())
                    buffer.putShort(cfg.value.size.toShort())
                    buffer.put(cfg.value)
                } else {
                    val len = cfg.value.size + 2
                    buffer.put(len.toByte())
                    buffer.putShort(cfg.type.toShort())
                    buffer.put(cfg.value)
                }
            }

            return buffer.array()
        }
    }

    fun toByteArray(): ByteArray {
        return toByteArray(listOf(this))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CommonConfig
        return type == other.type && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + value.contentHashCode()
        return result
    }

    override fun toString(): String {
        val hexVal = value.joinToString(" ") { "%02X".format(it) }
        return "CommonConfig(type=$type, len=${value.size}, val=[$hexVal])"
    }
}
