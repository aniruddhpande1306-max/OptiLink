package com.optilink.app.model

/**
 * Represents a single optical frame to be displayed and transmitted.
 */
data class TransferFrame(
    val transferId: Int,
    val frameNumber: Int,
    val totalFrames: Int,
    val payloadLength: Int,
    val payload: ByteArray,
    val checksum: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransferFrame

        if (transferId != other.transferId) return false
        if (frameNumber != other.frameNumber) return false
        if (totalFrames != other.totalFrames) return false
        if (payloadLength != other.payloadLength) return false
        if (!payload.contentEquals(other.payload)) return false
        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transferId
        result = 31 * result + frameNumber
        result = 31 * result + totalFrames
        result = 31 * result + payloadLength
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + checksum
        return result
    }
}
