package com.onlylipu.cloud.streaming

data class StreamStats(
    val pingMs: Int = 0,
    val fps: Int = 0,
    val bitrateKbps: Int = 0,
    val packetLossPct: Double = 0.0,
    val resolution: String = "-",
    val state: String = "disconnected"
)

enum class StreamQuality(val label: String, val maxHeight: Int, val maxFps: Int, val maxBitrateKbps: Int) {
    AUTO("Auto", 0, 0, 0),
    DATA_SAVER("Data Saver", 540, 30, 800),
    HD_720("720p 60 FPS", 720, 60, 4000),
    FHD_1080("1080p 60 FPS", 1080, 60, 8000),
    HIGHEST("Highest", 1440, 60, 16000)
}
