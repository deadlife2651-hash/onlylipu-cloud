package com.onlylipu.cloud.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.onlylipu.cloud.BuildConfig
import com.onlylipu.cloud.data.auth.TokenStore
import com.onlylipu.cloud.ui.theme.Graphite800
import com.onlylipu.cloud.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val store = remember { TokenStore(context) }
    var biometric by remember { mutableStateOf(store.biometricEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SettingsGroup("Connection") {
                SettingsRow(Icons.Default.Wifi, "Network and connection",
                    "Server: ${BuildConfig.SERVER_BASE_URL}")
                SettingsRow(Icons.Default.Hd, "Display and streaming quality",
                    "Default: ${store.defaultQuality}")
                SettingsRow(Icons.Default.VolumeUp, "Audio", "Stream system audio")
            }
            SettingsGroup("Environments") {
                SettingsRow(Icons.Default.Computer, "Cloud Computer",
                    "Desktop streaming preferences") { nav.navigate("session/computer") }
                SettingsRow(Icons.Default.Android, "Cloud Android",
                    "Manage apps and storage") { nav.navigate("apps") }
                SettingsRow(Icons.Default.Wallpaper, "Wallpaper and appearance",
                    "Dark / light mode")
                SettingsRow(Icons.Default.Storage, "Storage", "Persistent cloud storage")
            }
            SettingsGroup("Security") {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Biometric unlock", fontWeight = FontWeight.Medium)
                        Text("Require fingerprint to open the app",
                            color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(checked = biometric, onCheckedChange = {
                        biometric = it
                        store.biometricEnabled = it
                    })
                }
                SettingsRow(Icons.Default.Security, "Security", "Session timeout, audit log")
                SettingsRow(Icons.Default.Logout, "Log out from all devices",
                    "Ends every active session") {
                    store.clear()
                    nav.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            }
            SettingsGroup("About") {
                SettingsRow(Icons.Default.Info, "About OnlyLipu Cloud",
                    "Version ${BuildConfig.VERSION_NAME} • com.onlylipu.cloud")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = Graphite800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
