package com.example.snispoofing.data.model

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

object TlsClientHelloMaker {
    const val CLIENT_HELLO_LEN = 519
    const val MAX_SNI_LEN = 219

    private val TEMPLATE_HEX =
        "1603010200010001fc030341d5b549d9cd1adfa7296c8418d157dc7b624c842824ff493b9375bb48d34f2b20bf018bcc90a7c89a230094815ad0c15b736e38c01209d72d282cb5e2105328150024130213031301c02cc030c02bc02fcca9cca8c024c028c023c027009f009e006b006700ff0100018f0000000b00090000066d63692e6972000b000403000102000a00160014001d0017001e0019001801000101010201030104002300000010000e000c02683208687474702f312e310016000000170000000d002a0028040305030603080708080809080a080b080408050806040105010601030303010302040205020602002b00050403040303002d00020101003300260024001d0020435bacc4d05f9d41fef44ab3ad55616c36e0613473e2338770efdaa98693d217001500d5000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"

    private val templateBytes: ByteArray = hexToBytes(TEMPLATE_HEX)
    private val static1 = templateBytes.copyOfRange(0, 11)
    private val static2 = byteArrayOf(0x20.toByte())
    private val static3 = templateBytes.copyOfRange(76, 120)
    private val templateSniLen = 6 // "mci.ir"
    private val static4 = templateBytes.copyOfRange(127 + templateSniLen, 262 + templateSniLen)
    private val static5 = byteArrayOf(0x00.toByte(), 0x15.toByte())

    private val secureRandom = SecureRandom()

    fun getClientHelloWith(
        random: ByteArray,
        sessionId: ByteArray,
        targetSni: String,
        keyShare: ByteArray
    ): ByteArray {
        val safeSni = if (targetSni.isBlank()) "mci.ir" else targetSni
        val rawBytes = safeSni.toByteArray(Charsets.UTF_8)
        val sniBytes = if (rawBytes.size > MAX_SNI_LEN) rawBytes.copyOf(MAX_SNI_LEN) else rawBytes
        val sniLen = sniBytes.size

        // Server Name Extension header: extension_type (0x0000), extension_length, server_name_list_length, server_name_type (0x00), host_name_length
        val sniExtension = ByteBuffer.allocate(9 + sniLen).apply {
            order(ByteOrder.BIG_ENDIAN)
            putShort((sniLen + 5).toShort())
            putShort((sniLen + 3).toShort())
            put(0x00.toByte())
            putShort(sniLen.toShort())
            put(sniBytes)
        }.array()

        val padLen = maxOf(0, MAX_SNI_LEN - sniLen)
        val paddingExtension = ByteBuffer.allocate(2 + padLen).apply {
            order(ByteOrder.BIG_ENDIAN)
            putShort(padLen.toShort())
            put(ByteArray(padLen))
        }.array()

        val totalLen = static1.size + random.size + static2.size + sessionId.size + static3.size + sniExtension.size + static4.size + keyShare.size + static5.size + paddingExtension.size
        val output = ByteBuffer.allocate(totalLen)
        output.put(static1)
        output.put(random)
        output.put(static2)
        output.put(sessionId)
        output.put(static3)
        output.put(sniExtension)
        output.put(static4)
        output.put(keyShare)
        output.put(static5)
        output.put(paddingExtension)

        return output.array()
    }

    fun generateRandomDecoy(targetSni: String): ByteArray {
        val random = ByteArray(32)
        val sessionId = ByteArray(32)
        val keyShare = ByteArray(32)
        secureRandom.nextBytes(random)
        secureRandom.nextBytes(sessionId)
        secureRandom.nextBytes(keyShare)
        return getClientHelloWith(random, sessionId, targetSni, keyShare)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun bytesToHex(bytes: ByteArray, maxBytes: Int = bytes.size): String {
        val sb = StringBuilder()
        val limit = minOf(bytes.size, maxBytes)
        for (i in 0 until limit) {
            sb.append(String.format("%02X ", bytes[i]))
        }
        if (limit < bytes.size) {
            sb.append("... (${bytes.size - limit} more bytes)")
        }
        return sb.toString().trim()
    }
}
