package dev.paraspatil.luminaai.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.paraspatil.luminaai.data.local.MessageSender
import dev.paraspatil.luminaai.domain.pipeline.ChatState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val chatHistory by viewModel.chatHistory.collectAsState()
    val chatState by viewModel.chatState.collectAsState()

    // UI States
    var isKeyboardOpen by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var audioAmplitude by remember { mutableFloatStateOf(0f) }

    // Helpers
    val audioRecorderHelper = remember { AudioRecorderHelper() }
    val listState = rememberLazyListState()

    val isAtEnd by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(isAtEnd) {
        if (isAtEnd && chatHistory.size >= 20) {
            viewModel.loadMoreMessages()
        }
    }

    // ✅ COLLECT AUDIO AMPLITUDE WHEN LISTENING
    LaunchedEffect(isListening) {
        if (isListening) {
            try {
                audioRecorderHelper.getAmplitudeFlow().collect { amplitude ->
                    audioAmplitude = amplitude
                }
            } catch (e: Exception) {
                e.printStackTrace()
                audioAmplitude = 0f
            }
        } else {
            audioAmplitude = 0f  // Reset when not listening
        }
    }

    // Microphone Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = !isListening
            isKeyboardOpen = false
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                // Custom Slide-Up Keyboard Input
                AnimatedVisibility(
                    visible = isKeyboardOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        IconButton(onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            isKeyboardOpen = false
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Bottom Action Bar (Mic & Keyboard toggles)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                isListening = !isListening
                                isKeyboardOpen = false
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(32.dp)
                            )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Mic", modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    IconButton(onClick = {
                        isKeyboardOpen = !isKeyboardOpen
                        isListening = false
                    }) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Keyboard", modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp)
            ) {
                // Parallax Aura Circle Header
                item {
                    val isFirstItemVisible by remember {
                        derivedStateOf { listState.firstVisibleItemIndex == 0 }
                    }
                    val scrollOffset = if (isFirstItemVisible) listState.firstVisibleItemScrollOffset else 1000

                    // Fade out and translate up based on scroll
                    val auraAlpha = (1f - (scrollOffset / 500f)).coerceIn(0f, 1f)
                    val auraTranslationY = (scrollOffset * 0.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .graphicsLayer {
                                alpha = auraAlpha
                                translationY = auraTranslationY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AuraCircle(
                            isListening = isListening,
                            amplitude = audioAmplitude  // ✅ NOW RECEIVES REAL AMPLITUDE
                        )
                    }
                }
                item {
                    if (chatState !is ChatState.Idle) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (chatState is ChatState.Error) {
                                    (chatState as ChatState.Error).errorMessage
                                } else {
                                    "Status: ${chatState::class.simpleName}"
                                },
                                modifier = Modifier.alpha(if (chatState is ChatState.Error) 1f else 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (chatState is ChatState.Error) MaterialTheme.colorScheme.error else Color.Unspecified
                            )

                            // Add the Retry Option Requirement
                            if (chatState is ChatState.Error) {
                                TextButton(onClick = {
                                    // Retry the last message sent
                                    chatHistory.firstOrNull()?.let { lastMsg ->
                                        viewModel.sendMessage(lastMsg.messageText)
                                    }
                                }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                // Chat History List
                items(chatHistory.reversed()) { message ->
                    val isUser = message.sender == MessageSender.USER

                    // Format the timestamp (e.g., 10:45 AM)
                    val formattedTime = remember(message.timestamp) {
                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(message.timestamp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                                .widthIn(max = 250.dp)
                        ) {
                            Text(
                                text = message.messageText,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Timestamp Text
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}