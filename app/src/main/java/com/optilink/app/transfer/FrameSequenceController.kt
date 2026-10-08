package com.optilink.app.transfer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.optilink.app.model.TransferFrame
import kotlinx.coroutines.*

class FrameSequenceController(
    val frames: List<TransferFrame>,
    val config: OpticalTransferConfig = OpticalTransferConfig(),
    val coroutineScope: CoroutineScope
) {
    var currentState by mutableStateOf(TransmissionState.IDLE)
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var countdownValue by mutableStateOf("")
        private set

    private var job: Job? = null

    val currentFrame: TransferFrame?
        get() = frames.getOrNull(currentIndex)

    val totalFrames: Int = frames.size

    val progress: Float
        get() = when {
            totalFrames == 0 -> 0f
            currentState == TransmissionState.FINISHED -> 1f
            else -> currentIndex.toFloat() / totalFrames
        }

    val isRunning: Boolean
        get() = currentState == TransmissionState.TRANSMITTING || currentState == TransmissionState.PREPARING

    fun start() {
        if (frames.isEmpty()) {
            currentState = TransmissionState.FINISHED
            return
        }
        if (currentState == TransmissionState.IDLE || currentState == TransmissionState.CANCELLED || currentState == TransmissionState.FINISHED) {
            currentIndex = 0
            startCountdownThenTransmit()
        }
    }

    fun resume() {
        if (currentState == TransmissionState.PAUSED) {
            startTransmissionLoop()
        }
    }

    fun pause() {
        if (currentState == TransmissionState.TRANSMITTING || currentState == TransmissionState.PREPARING) {
            job?.cancel()
            currentState = TransmissionState.PAUSED
        }
    }

    fun stop() {
        job?.cancel()
        currentState = TransmissionState.CANCELLED
    }

    fun nextFrame() {
        if (currentIndex < frames.size - 1) {
            currentIndex++
        }
    }

    fun previousFrame() {
        if (currentIndex > 0) {
            currentIndex--
        }
    }

    private fun startCountdownThenTransmit() {
        job?.cancel()
        currentState = TransmissionState.PREPARING
        job = coroutineScope.launch {
            for (i in config.countdownTicks downTo 1) {
                countdownValue = i.toString()
                delay(1000L)
            }
            countdownValue = "START"
            delay(500L) // Show START briefly
            startTransmissionLoop()
        }
    }

    private fun startTransmissionLoop() {
        currentState = TransmissionState.TRANSMITTING
        job = coroutineScope.launch {
            while (currentIndex < frames.size) {
                // Display frame perfectly stable for the entire exact frame duration parameter with NO global limits
                delay(config.frameDurationMs)
                
                if (currentIndex < frames.size - 1) {
                    currentIndex++
                } else {
                    break
                }
            }
            
            currentState = TransmissionState.FINISHED
        }
    }
}
