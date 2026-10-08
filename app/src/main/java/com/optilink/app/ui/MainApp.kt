package com.optilink.app.ui

import androidx.compose.runtime.*
import com.optilink.app.ui.navigation.Screen
import com.optilink.app.ui.screens.HomeScreen
import com.optilink.app.ui.screens.HowItWorksScreen
import com.optilink.app.ui.screens.SendTextScreen
import com.optilink.app.ui.screens.TransferPreviewScreen
import com.optilink.app.transfer.FrameSequenceController
import com.optilink.app.encoder.TextEncoder

@Composable
fun MainApp() {
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var frameController by remember { mutableStateOf<FrameSequenceController?>(null) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onNavigateToSendText = { currentScreen = Screen.SendText },
            onNavigateToHowItWorks = { currentScreen = Screen.HowItWorks }
        )
        Screen.SendText -> SendTextScreen(
            onNavigateBack = { currentScreen = Screen.Home },
            onStartTransfer = { text, speedMs ->
                val frames = TextEncoder.encodeText(text)
                frameController = FrameSequenceController(
                    frames = frames,
                    config = com.optilink.app.transfer.OpticalTransferConfig(frameDurationMs = speedMs),
                    coroutineScope = scope
                )
                currentScreen = Screen.TransferPreview
            }
        )
        Screen.HowItWorks -> HowItWorksScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.TransferPreview -> {
            frameController?.let { controller ->
                TransferPreviewScreen(
                    controller = controller,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            } ?: run {
                currentScreen = Screen.Home
            }
        }
    }
}
