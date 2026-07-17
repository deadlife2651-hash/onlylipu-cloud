package com.onlylipu.cloud.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.onlylipu.cloud.streaming.StreamQuality
import com.onlylipu.cloud.ui.theme.Graphite800
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@Composable
fun SessionScreen(
    nav: NavHostController,
    mode: String,
    vm: SessionViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    var showKeyboard by remember { mutableStateOf(false) }
    var qualityMenu by remember { mutableStateOf(false) }
    var mouseMode by remember { mutableStateOf(mode == "computer") }

    LaunchedEffect(mode) { vm.start(mode) }
    LaunchedEffect(state.disconnected) {
        if (state.disconnected) nav.popBackStack()
    }
    LaunchedEffect(state.showControls) {
        if (state.showControls) {
            delay(3500)
            vm.toggleControls().invoke()
        }
    }

    BackHandler { vm.disconnect() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(mouseMode) {
                if (mouseMode) {
                    // Computer mode: tap = left click, long press = right click
                    detectTapGestures(
                        onTap = { o ->
                            vm.sendTouch("click", o.x / size.width, o.y / size.height)
                        },
                        onLongPress = { o ->
                            vm.sendTouch("rightclick", o.x / size.width, o.y / size.height)
                        }
                    )
                } else {
                    detectTapGestures(
                        onTap = { o ->
                            vm.sendTouch("tap", o.x / size.width, o.y / size.height)
                        },
                        onLongPress = { o ->
                            vm.sendTouch("longpress", o.x / size.width, o.y / size.height)
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    if (pan.x != 0f || pan.y != 0f) vm.sendScroll(pan.x, pan.y)
                }
            }
    ) {
        // Full-screen remote video — Cloud Android streams directly,
        // never inside a desktop window or emulator frame.
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).also { vm.attachRenderer(it) }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Connecting state
        if (state.connectionState != "connected") {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                Text(
                    when (state.connectionState) {
                        "starting" -> "Starting your cloud environment…"
                        "checking", "new" -> "Connecting securely…"
                        else -> state.connectionState.replaceFirstChar { it.uppercase() }
                    },
                    color = Color.White, fontSize = 15.sp
                )
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        // Live diagnostics overlay
        if (state.showOverlay) {
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0xAA000000), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                val s = state.stats
                OverlayLine("Ping", "${s.pingMs} ms")
                OverlayLine("FPS", "${s.fps}")
                OverlayLine("Bitrate", "${s.bitrateKbps} kbps")
                OverlayLine("Loss", "%.1f%%".format(s.packetLossPct))
                OverlayLine("Resolution", s.resolution)
                OverlayLine("State", state.connectionState)
            }
        }

        // Auto-hiding floating controls
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .background(Graphite800.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.disconnect() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                IconButton(onClick = { mouseMode = !mouseMode }) {
                    Icon(
                        if (mouseMode) Icons.Default.Mouse else Icons.Default.TouchApp,
                        "Input mode", tint = Color.White
                    )
                }
                IconButton(onClick = { showKeyboard = true }) {
                    Icon(Icons.Default.Keyboard, "Keyboard", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { qualityMenu = true }) {
                        Icon(Icons.Default.Hd, "Quality", tint = Color.White)
                    }
                    DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                        StreamQuality.entries.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q.label) },
                                onClick = { vm.setQuality(q); qualityMenu = false }
                            )
                        }
                    }
                }
                IconButton(onClick = { vm.toggleAudio() }) {
                    Icon(
                        if (state.audioEnabled) Icons.AutoMirrored.Filled.VolumeUp
                        else Icons.AutoMirrored.Filled.VolumeOff,
                        "Audio", tint = Color.White
                    )
                }
                IconButton(onClick = { vm.toggleOverlay() }) {
                    Icon(Icons.Default.Info, "Stats", tint = Color.White)
                }
            }
        }
    }

    if (showKeyboard) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKeyboard = false },
            title = { Text("Remote keyboard") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type to send to cloud…") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotEmpty()) vm.sendText(text)
                    text = ""
                    showKeyboard = false
                }) { Text("Send") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.sendKey("back"); showKeyboard = false }) {
                        Text("Back")
                    }
                    TextButton(onClick = { vm.sendKey("home"); showKeyboard = false }) {
                        Text("Home")
                    }
                    TextButton(onClick = { vm.sendKey("recent"); showKeyboard = false }) {
                        Text("Recent")
                    }
                }
            }
        )
    }
}

@Composable
private fun OverlayLine(label: String, value: String) {
    Row {
        Text(label, color = Color(0xFF9AA7B8), fontSize = 11.sp, modifier = Modifier.width(70.dp))
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
