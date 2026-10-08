package com.optilink.app.encoder

import com.optilink.app.model.TransferFrame
import com.optilink.app.utils.ChecksumUtils
import kotlin.random.Random

object TextEncoder {
    
    // Max 53 bytes per frame (determined by optical grid size limitation 24x24 - header - finders)
    const val MAX_PAYLOAD_SIZE = 53

    /**
     * Converts a string into a sequence of TransferFrames.
     * Preserves UTF-8.
     */
    fun encodeText(text: String): List<TransferFrame> {
        val transferId = Random.nextInt(1, Int.MAX_VALUE) // Positive random ID
        val bytes = text.toByteArray(Charsets.UTF_8)
        
        if (bytes.isEmpty()) {
            return listOf(
                TransferFrame(
                    transferId = transferId,
                    frameNumber = 1,
                    totalFrames = 1,
                    payloadLength = 0,
                    payload = ByteArray(0),
                    checksum = ChecksumUtils.calculateAdler32(ByteArray(0))
                )
            )
        }

        // Chunk bytes
        val chunks = bytes.toList().chunked(MAX_PAYLOAD_SIZE).map { it.toByteArray() }
        val totalFrames = chunks.size

        return chunks.mapIndexed { index, chunk ->
            val frameNumber = index + 1
            
            // Generate checksum for payload (in real complete protocol, we might hash the header too)
            val checksum = ChecksumUtils.calculateAdler32(chunk)

            TransferFrame(
                transferId = transferId,
                frameNumber = frameNumber,
                totalFrames = totalFrames,
                payloadLength = chunk.size,
                payload = chunk,
                checksum = checksum
            )
        }
    }
}
