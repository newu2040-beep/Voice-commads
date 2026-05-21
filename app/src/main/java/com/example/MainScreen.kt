package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import com.example.voice.data.CommandHistory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voice.data.CustomCommand
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeIndex by viewModel.themeIndex.collectAsState()
    val voiceIndex by viewModel.voiceIndex.collectAsState()
    val customCommands by viewModel.allCustomCommands.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCustomCommandsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA
        ).let { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it + Manifest.permission.POST_NOTIFICATIONS
            } else {
                it
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                           Icon(imageVector = Icons.Filled.Mic, contentDescription = null, tint = Color.White) 
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AURA AI", 
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (Settings.canDrawOverlays(context)) {
                            val intent = Intent(context, com.example.service.FloatingVoiceService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Start Floating Bubble", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(imageVector = Icons.Filled.List, contentDescription = "History", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { showCustomCommandsDialog = true }) {
                        Icon(imageVector = Icons.Filled.Build, contentDescription = "Custom Commands", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (permissionsState.allPermissionsGranted) {
            VoiceAssistantContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() }
            )
        } else {
            PermissionRequestContent(
                modifier = Modifier.padding(paddingValues),
                permissionsState = permissionsState
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                isDarkMode = isDarkMode,
                themeIndex = themeIndex,
                voiceIndex = voiceIndex,
                onDismiss = { showSettingsDialog = false },
                onDarkModeToggle = { viewModel.setDarkMode(it) },
                onThemeSelected = { viewModel.setTheme(it) },
                onVoiceSelected = { viewModel.setVoiceType(it) },
                onVoicePreview = { viewModel.previewVoice(it) },
                context = context
            )
        }

        if (showCustomCommandsDialog) {
            CustomCommandsDialog(
                commands = customCommands,
                onDismiss = { showCustomCommandsDialog = false },
                onAddCommand = { phrase, type, data -> viewModel.addCustomCommand(phrase, type, data) },
                onDeleteCommand = { id -> viewModel.deleteCustomCommand(id) }
            )
        }

        if (showHistoryDialog) {
            HistoryDialog(
                history = commandHistory,
                onDismiss = { showHistoryDialog = false },
                onClearHistory = { viewModel.clearHistory() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantContent(
    modifier: Modifier = Modifier,
    uiState: VoiceUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Status string
        Text(
            text = "STATUS: ${uiState.statusText.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Recognized Text
        val words = uiState.recognizedText.ifEmpty { "Say a command" }.uppercase().split(" ")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            words.forEachIndexed { index, word ->
                Text(
                    text = word,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp,
                        fontSize = 48.sp,
                        lineHeight = 44.sp
                    ),
                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Voice bars (only if listening)
        if (uiState.isListening) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animate bars
                val transition = rememberInfiniteTransition(label = "bars")
                val animations = (0..4).map { i ->
                    transition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, delayMillis = i * 100, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$i"
                    )
                }

                val baseHeights = listOf(24.dp, 40.dp, 56.dp, 40.dp, 24.dp)
                baseHeights.forEachIndexed { index, height ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(height * animations[index].value)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(84.dp)) // Placeholder height for max animation height
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Response string
        if (uiState.responseMessage.isNotEmpty()) {
             Text(
                text = uiState.responseMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // Suggested commands
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "SUGGESTED COMMANDS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("\"Turn on Flashlight\"", "\"Open Gallery\"", "\"Call Sarah\"").forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .shadow(2.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Mic FAB area
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 32.dp)) {
            // Background ripple circle
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (uiState.isListening) 1.5f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .graphicsLayer { 
                        scaleX = scale
                        scaleY = scale
                    }
            )

            // Mic FAB
            FloatingActionButton(
                onClick = {
                    if (uiState.isListening) {
                        onStopListening()
                    } else {
                        onStartListening()
                    }
                },
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                containerColor = if (uiState.isListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = if (uiState.isListening) "Stop Listening" else "Start Listening",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    isDarkMode: Boolean,
    themeIndex: Int,
    voiceIndex: Int,
    onDismiss: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onThemeSelected: (Int) -> Unit,
    onVoiceSelected: (Int) -> Unit,
    onVoicePreview: (Int) -> Unit,
    context: android.content.Context
) {
    val themes = listOf("Ocean", "Emerald", "Violet", "Rose", "Sunset", "Slate", "Midnight", "Neon", "Mint")
    val voices = listOf("Default", "Sweet", "Mature", "Friendly", "Calm", "Natural")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            LazyColumn {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Dark Mode", modifier = Modifier.weight(1f))
                        Switch(checked = isDarkMode, onCheckedChange = onDarkModeToggle)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent2 = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent2)
                                } catch (e2: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Background Execution")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Voice Assistant Identity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(voices.size) { index ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onVoiceSelected(index)
                                onVoicePreview(index)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = voiceIndex == index,
                            onClick = {
                                onVoiceSelected(index)
                                onVoicePreview(index)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(voices[index], modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { onVoicePreview(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Preview voice ${voices[index]}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("App Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(themes.size) { index ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(index) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = themeIndex == index,
                            onClick = { onThemeSelected(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(themes[index])
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Made with love by Editingcells", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CustomCommandsDialog(
    commands: List<CustomCommand>,
    onDismiss: () -> Unit,
    onAddCommand: (String, String, String) -> Unit,
    onDeleteCommand: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Commands") },
        text = {
            if (commands.isEmpty()) {
                Text("No custom commands defined yet.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(commands) { command ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(command.phrase, fontWeight = FontWeight.Bold)
                                    Text("${command.actionType}: ${command.actionData}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onDeleteCommand(command.id) }) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showAddDialog = true }) {
                Text("Add Command")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showAddDialog) {
        var phrase by remember { mutableStateOf("") }
        var actionType by remember { mutableStateOf("RESPONSE") }
        var actionData by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Command") },
            text = {
                Column {
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { phrase = it },
                        label = { Text("Command Phrase") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Action Type", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = actionType == "RESPONSE", onClick = { actionType = "RESPONSE" })
                        Text("Response", modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = actionType == "APP", onClick = { actionType = "APP" })
                        Text("App (Package)")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = actionData,
                        onValueChange = { actionData = it },
                        label = { Text(if (actionType == "APP") "App Name/Package" else "Text to Speak") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (phrase.isNotBlank() && actionData.isNotBlank()) {
                        onAddCommand(phrase.trim(), actionType, actionData.trim())
                        showAddDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun HistoryDialog(
    history: List<CommandHistory>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Command History") },
        text = {
            if (history.isEmpty()) {
                Text("No voice commands yet.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.7f)) {
                    items(history) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.commandText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.responseText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(item.getFormattedTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClearHistory) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestContent(
    modifier: Modifier = Modifier,
    permissionsState: MultiplePermissionsState
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textToShow = if (permissionsState.shouldShowRationale) {
            "The voice assistant requires microphone, camera, and contact permissions to execute commands. Please grant them."
        } else {
            "Voice permissions are required for the assistant to function. Please grant them."
        }
        
        Text(
             text = textToShow,
             textAlign = TextAlign.Center,
             style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                if (permissionsState.shouldShowRationale || !permissionsState.allPermissionsGranted) {
                    permissionsState.launchMultiplePermissionRequest()
                } else {
                    // Open settings if fully denied
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        ) {
            Text("Grant Permissions")
        }
    }
}
