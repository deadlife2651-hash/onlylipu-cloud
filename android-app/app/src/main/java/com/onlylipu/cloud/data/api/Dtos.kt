package com.onlylipu.cloud.data.api

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresIn: Long = 0,
    val message: String = ""
)

@Serializable
data class ServerStatus(
    val online: Boolean = false,
    val state: String = "offline",          // online | offline | starting | connecting
    val location: String = "Tokyo, JP",
    val cpuPercent: Double = 0.0,
    val ramUsedGb: Double = 0.0,
    val ramTotalGb: Double = 32.0,
    val storageUsedGb: Double = 0.0,
    val storageTotalGb: Double = 480.0,
    val cloudAndroidRunning: Boolean = false,
    val cloudComputerRunning: Boolean = false,
    val activeSessions: Int = 0
)

@Serializable
data class InstalledApp(
    val packageName: String,
    val label: String = "",
    val versionName: String = "",
    val sizeMb: Double = 0.0,
    val installedAt: String = ""
)

@Serializable
data class AppsResponse(val apps: List<InstalledApp> = emptyList())

@Serializable
data class ActionResponse(val ok: Boolean = false, val message: String = "")

@Serializable
data class ApiError(val error: String = "unknown_error")
