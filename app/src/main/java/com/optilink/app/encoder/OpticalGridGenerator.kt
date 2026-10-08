package com.optilink.app.encoder

import com.optilink.app.model.TransferFrame

/**
 * Utility to generate a deterministic 24x24 logical 2D grid from a TransferFrame.
 * Output is a 1D Boolean array of size 24*24 = 576 representing (x, y) pixels.
 * True = White (data 1), False = Black (data 0).
 */
object OpticalGridGenerator {

    const val GRID_SIZE = 24
    
    // Convert int/short into bytes
    private fun Int.toBytes(): ByteArray {
        return byteArrayOf(
            ((this shr 24) and 0xFF).toByte(),
            ((this shr 16) and 0xFF).toByte(),
            ((this shr 8) and 0xFF).toByte(),
            (this and 0xFF).toByte()
        )
    }

    private fun Int.toShortBytes(): ByteArray {
        return byteArrayOf(
            ((this shr 8) and 0xFF).toByte(),
            (this and 0xFF).toByte()
        )
    }

    private fun isFinderPattern(x: Int, y: Int): Boolean {
        // Top-Left 4x4
        if (x < 4 && y < 4) return true
        // Top-Right 4x4
        if (x >= GRID_SIZE - 4 && y < 4) return true
        // Bottom-Left 4x4
        if (x < 4 && y >= GRID_SIZE - 4) return true
        return false
    }

    fun generateGrid(frame: TransferFrame): BooleanArray {
        val grid = BooleanArray(GRID_SIZE * GRID_SIZE)
        
        // 1. Draw Finder Patterns (Outer black border, inner white)
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (isFinderPattern(x, y)) {
                    // Central 2x2 of the 4x4 is white (true)
                    val isTL = x in 1..2 && y in 1..2
                    val isTR = x in (GRID_SIZE - 3)..(GRID_SIZE - 2) && y in 1..2
                    val isBL = x in 1..2 && y in (GRID_SIZE - 3)..(GRID_SIZE - 2)
                    grid[y * GRID_SIZE + x] = isTL || isTR || isBL
                }
            }
        }

        // 2. Prepare Data Buffer
        // transferId(4) + frame(2) + totalFrames(2) + payloadLength(1) + checksum(4) = 13 bytes
        val dataBytes = ByteArray(13 + frame.payloadLength)
        
        System.arraycopy(frame.transferId.toBytes(), 0, dataBytes, 0, 4)
        System.arraycopy(frame.frameNumber.toShortBytes(), 0, dataBytes, 4, 2)
        System.arraycopy(frame.totalFrames.toShortBytes(), 0, dataBytes, 6, 2)
        dataBytes[8] = frame.payloadLength.toByte()
        System.arraycopy(frame.checksum.toBytes(), 0, dataBytes, 9, 4)
        
        System.arraycopy(frame.payload, 0, dataBytes, 13, frame.payloadLength)

        // 3. Map bits into the grid avoiding finder patterns
        var bitIndex = 0
        val totalBits = dataBytes.size * 8
        
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (!isFinderPattern(x, y)) {
                    if (bitIndex < totalBits) {
                        val byteIndex = bitIndex / 8
                        val bitOffset = 7 - (bitIndex % 8) // MSB first
                        val bit = ((dataBytes[byteIndex].toInt() shr bitOffset) and 1) == 1
                        grid[y * GRID_SIZE + x] = bit
                        bitIndex++
                    } else {
                        // padding with alternating bits for visual texture
                        grid[y * GRID_SIZE + x] = (x + y) % 2 == 0
                    }
                }
            }
        }
        
        return grid
    }
}
