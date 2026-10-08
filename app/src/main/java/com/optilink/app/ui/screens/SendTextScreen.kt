package com.optilink.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTextScreen(
    onNavigateBack: () -> Unit,
    onStartTransfer: (String, Long) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedSpeed by remember { mutableStateOf(500L) }
    val speedOptions = listOf(250L, 500L, 750L, 1000L)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Text") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Enter the text to transfer. The transfer will use the phone display and visible light.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Type here...") },
                supportingText = { Text("${text.length} characters") },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Text("X")
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Test Mode Frame Duration:", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                speedOptions.forEach { speed ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = speed == selectedSpeed,
                            onClick = { selectedSpeed = speed }
                        )
                        Text("$speed ms", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onStartTransfer(text, selectedSpeed) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Transfer")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    onStartTransfer("Hello OptiLink", selectedSpeed)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("SHOW TEST OPTICAL FRAME")
            }
        }
    }
}
