package com.onlylipu.cloud.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.onlylipu.cloud.navigation.Routes
import com.onlylipu.cloud.ui.theme.ErrorRed
import com.onlylipu.cloud.ui.theme.Graphite800
import com.onlylipu.cloud.ui.theme.SuccessGreen
import com.onlylipu.cloud.ui.theme.TextSecondary
import com.onlylipu.cloud.ui.theme.WarningAmber

@Composable
fun DashboardScreen(nav: NavHostController, vm: DashboardViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) {
            nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("OnlyLipu Cloud", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Your Private Computer. Your Cloud Android.",
                        color = TextSecondary, fontSize = 12.sp)
                }
                Row {
                    IconButton(onClick = { nav.navigate(Routes.APPS) }) {
                        Icon(Icons.Default.Android, "Apps", tint = TextSecondary)
                    }
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
                    }
                    IconButton(onClick = { vm.logout() }) {
                        Icon(Icons.Default.Logout, "Logout", tint = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            StatusBar(state, onRetry = { vm.startCloudAndroid() })

            Spacer(Modifier.height(24.dp))
            EnvironmentCard(
                title = "Cloud Computer",
                subtitle = "Your dedicated desktop",
                icon = Icons.Default.Computer,
                running = state.status.cloudComputerRunning,
                enabled = state.status.online
            ) {
                vm.markUsed("computer")
                nav.navigate(Routes.session("computer"))
            }
            Spacer(Modifier.height(16.dp))
            EnvironmentCard(
                title = "Cloud Android",
                subtitle = "Your private Android phone",
                icon = Icons.Default.Android,
                running = state.status.cloudAndroidRunning,
                enabled = state.status.online
            ) {
                vm.markUsed("android")
                nav.navigate(Routes.session("android"))
            }

            Spacer(Modifier.height(24.dp))
            ResourcePanel(state)

            vm.lastEnvironment?.let { env ->
                Spacer(Modifier.height(20.dp))
                Card(
                    Modifier.fillMaxWidth().clickable {
                        nav.navigate(Routes.session(env))
                    },
                    colors = CardDefaults.cardColors(containerColor = Graphite800),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Reconnect", fontWeight = FontWeight.SemiBold)
                            Text("Last session: ${if (env == "android") "Cloud Android" else "Cloud Computer"}",
                                color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        state.actionMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = { vm.clearActionMessage() }) { Text("OK") } }
            ) { Text(msg) }
        }
    }
}

@Composable
private fun StatusBar(state: DashboardUiState, onRetry: () -> Unit) {
    val (color, label) = when {
        state.loading -> WarningAmber to "Connecting…"
        state.status.online -> SuccessGreen to "Online"
        else -> ErrorRed to "Offline"
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Graphite800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color)
                )
                Spacer(Modifier.width(10.dp))
                Text("Server: $label", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Speed, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (state.pingMs >= 0) "${state.pingMs} ms" else "—",
                    color = TextSecondary, fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Location: ${state.status.location}" +
                    (state.error?.let { "  •  $it" } ?: ""),
                color = TextSecondary, fontSize = 12.sp
            )
            if (!state.status.online && !state.loading) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) { Text("Wake server / start Cloud Android") }
            }
        }
    }
}

@Composable
private fun EnvironmentCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Graphite800 else Graphite800.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
            Text(
                if (running) "Running" else "Stopped",
                color = if (running) SuccessGreen else TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ResourcePanel(state: DashboardUiState) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Graphite800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Server resources", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            ResourceBar("CPU", (state.status.cpuPercent / 100f).toFloat(),
                "${state.status.cpuPercent.toInt()}%")
            Spacer(Modifier.height(10.dp))
            ResourceBar("RAM",
                (state.status.ramUsedGb / state.status.ramTotalGb).toFloat(),
                "${state.status.ramUsedGb.toInt()} / ${state.status.ramTotalGb.toInt()} GB")
            Spacer(Modifier.height(10.dp))
            ResourceBar("Storage",
                (state.status.storageUsedGb / state.status.storageTotalGb).toFloat(),
                "${state.status.storageUsedGb.toInt()} / ${state.status.storageTotalGb.toInt()} GB")
        }
    }
}

@Composable
private fun ResourceBar(label: String, fraction: Float, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(64.dp), color = TextSecondary, fontSize = 12.sp)
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary)
    }
}
