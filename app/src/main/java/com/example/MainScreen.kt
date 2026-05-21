package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA
        )
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
                listOf(24.dp, 40.dp, 56.dp, 40.dp, 24.dp).forEach { height ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(height)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(56.dp)) // Placeholder height
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
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
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
