package com.optilink.app.utils

object ChecksumUtils {
    /**
     * Implementing a simple pure Kotlin checksum (CRC-32 like or simple Adler-32).
     * Adler-32 is simple, fast, and deterministic without external libraries.
     */
    fun calculateAdler32(data: ByteArray): Int {
        var a = 1
        var b = 0
        for (byte in data) {
            val ubyte = byte.toInt() and 0xFF
            a = (a + ubyte) % 65521
            b = (b + a) % 65521
        }
        return (b shl 16) or a
    }
}
