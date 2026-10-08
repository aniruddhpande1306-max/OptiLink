package com.optilink.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.optilink.app.encoder.OpticalGridGenerator
import com.optilink.app.transfer.FrameSequenceController
import com.optilink.app.transfer.TransmissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferPreviewScreen(
    controller: FrameSequenceController,
    onNavigateBack: () -> Unit
) {
    val currentFrame = controller.currentFrame
    val state = controller.currentState
    val isTransmitting = state == TransmissionState.TRANSMITTING
    val isPreparing = state == TransmissionState.PREPARING
    
    // Screen Wake Lock
    val view = LocalView.current
    DisposableEffect(isTransmitting, isPreparing) {
        if (isTransmitting || isPreparing) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("<-")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentFrame != null) {
                // Large Grid Box securely padded in Black for OpenCV quad alignment
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.Black) // Solid optical isolation bounds
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val grid = remember(currentFrame) { OpticalGridGenerator.generateGrid(currentFrame) }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = OpticalGridGenerator.GRID_SIZE // 24
                        val drawnSize = 26 // Enforces exactly 1 virtual cell of universal quiet zone padding
                        val cellSizeW = size.width / drawnSize
                        val cellSizeH = size.height / drawnSize
                        
                        // Generates the White Quadrilateral specifically for laptop detection
                        drawRect(Color.White)
                        
                        for (y in 0 until gridSize) {
                            for (x in 0 until gridSize) {
                                val isWhite = grid[y * gridSize + x]
                                drawRect(
                                    color = if (isWhite) Color.White else Color.Black,
                                    topLeft = Offset((x + 1) * cellSizeW, (y + 1) * cellSizeH),
                                    size = Size(cellSizeW, cellSizeH)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val totalBytes = controller.frames.sumOf { it.payloadLength }
                
                Text(
                    text = "UTF-8 Bytes: $totalBytes | Frames: ${controller.totalFrames}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: $state",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isTransmitting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress mapped directly to Frame/Payload
                Text(
                    text = "Frame ${currentFrame.frameNumber} / ${controller.totalFrames}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { controller.progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tip: Maximize screen brightness for best results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state) {
                        TransmissionState.IDLE, TransmissionState.CANCELLED, TransmissionState.FINISHED -> {
                            Button(
                                onClick = { controller.start() },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) { Text(if (state == TransmissionState.FINISHED) "Restart" else "Start Transmission") }
                        }
                        TransmissionState.PREPARING, TransmissionState.TRANSMITTING -> {
                            Button(
                                onClick = { controller.pause() },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) { Text("Pause") }
                            OutlinedButton(
                                onClick = { controller.stop() },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) { Text("Cancel") }
                        }
                        TransmissionState.PAUSED -> {
                            Button(
                                onClick = { controller.resume() },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) { Text("Resume") }
                            OutlinedButton(
                                onClick = { controller.stop() },
                                modifier = Modifier.weight(1f).height(56.dp)
                            ) { Text("Cancel") }
                        }
                    }
                }
            } else {
                Text("No frames to preview.")
            }
        }
    }
}
