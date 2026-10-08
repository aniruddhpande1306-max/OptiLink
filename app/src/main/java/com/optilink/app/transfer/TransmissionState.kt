package com.optilink.app.transfer

enum class TransmissionState {
    IDLE,
    PREPARING,
    TRANSMITTING,
    PAUSED,
    FINISHED,
    CANCELLED
}

data class OpticalTransferConfig(
    val frameDurationMs: Long = 750L,
    val countdownTicks: Int = 3
)
