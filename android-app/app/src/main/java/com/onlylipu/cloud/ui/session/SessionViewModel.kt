package com.onlylipu.cloud.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlylipu.cloud.data.auth.TokenStore
import com.onlylipu.cloud.streaming.SignalingClient
import com.onlylipu.cloud.streaming.StreamQuality
import com.onlylipu.cloud.streaming.StreamStats
import com.onlylipu.cloud.streaming.WebRTCClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SurfaceViewRenderer

data class SessionUiState(
    val connectionState: String = "connecting",
    val stats: StreamStats = StreamStats(),
    val showControls: Boolean = true,
    val showOverlay: Boolean = false,
    val audioEnabled: Boolean = true,
    val quality: StreamQuality = StreamQuality.AUTO,
    val error: String? = null,
    val disconnected: Boolean = false
)

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenStore = TokenStore(app)

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state

    private var webrtc: WebRTCClient? = null
    private var signaling: SignalingClient? = null
    private var statsJob: Job? = null
    private var environment: String = "android"

    val eglContext get() = webrtc?.eglBase?.eglBaseContext

    fun start(env: String) {
        environment = env
        val token = tokenStore.token
        if (token.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "Session expired. Please sign in again.",
                disconnected = true)
            return
        }
        val client = WebRTCClient(getApplication(), object : WebRTCClient.Listener {
            override fun onLocalOffer(sdp: String) = signaling?.sendOffer(sdp) ?: Unit
            override fun onLocalIce(candidate: IceCandidate) =
                signaling?.sendIce(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp) ?: Unit
            override fun onStats(stats: StreamStats) {
                _state.value = _state.value.copy(stats = stats)
            }
            override fun onStateChanged(state: String) {
                _state.value = _state.value.copy(connectionState = state)
                if (state == "failed" || state == "disconnected") {
                    _state.value = _state.value.copy(error = "Connection lost. Reconnecting…")
                }
            }
        })
        webrtc = client
        client.init()

        signaling = SignalingClient(token, object : SignalingClient.Listener {
            override fun onAnswer(sdp: String) = webrtc?.onRemoteAnswer(sdp) ?: Unit
            override fun onIceCandidate(sdpMid: String?, sdpMLineIndex: Int, candidate: String) =
                webrtc?.addRemoteIce(sdpMid, sdpMLineIndex, candidate) ?: Unit
            override fun onServerEvent(event: String, payload: JSONObject) {
                when (event) {
                    "error" -> _state.value = _state.value.copy(
                        error = payload.optString("message", "Server error"))
                    "vm-starting" -> _state.value =
                        _state.value.copy(connectionState = "starting")
                }
            }
            override fun onClosed(reason: String) {
                _state.value = _state.value.copy(error = reason, disconnected = true)
            }
        }).also { it.connect(env) }

        client.connect()
        startStatsLoop()
    }

    fun attachRenderer(renderer: SurfaceViewRenderer) = webrtc?.attachRenderer(renderer)

    private fun startStatsLoop() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                webrtc?.pollStats()
            }
        }
    }

    fun sendTouch(action: String, xNorm: Float, yNorm: Float) {
        webrtc?.sendInput(
            """{"type":"touch","action":"$action","x":$xNorm,"y":$yNorm}"""
        )
    }

    fun sendScroll(dx: Float, dy: Float) =
        webrtc?.sendInput("""{"type":"scroll","dx":$dx,"dy":$dy}""")

    fun sendKey(key: String) =
        webrtc?.sendInput("""{"type":"key","key":"$key"}""")

    fun sendText(text: String) {
        val escaped = JSONObject.quote(text)
        webrtc?.sendInput("""{"type":"text","text":$escaped}""")
    }

    fun setQuality(q: StreamQuality) {
        _state.value = _state.value.copy(quality = q)
        if (q != StreamQuality.AUTO) {
            signaling?.sendQuality(q.maxHeight, q.maxFps, q.maxBitrateKbps)
        } else {
            signaling?.sendQuality(0, 0, 0)
        }
    }

    fun toggleAudio() {
        val newValue = !_state.value.audioEnabled
        webrtc?.setAudioEnabled(newValue)
        _state.value = _state.value.copy(audioEnabled = newValue)
    }

    fun toggleControls() =
        { _state.value = _state.value.copy(showControls = !_state.value.showControls) }

    fun toggleOverlay() {
        _state.value = _state.value.copy(showOverlay = !_state.value.showOverlay)
    }

    fun disconnect() {
        statsJob?.cancel()
        signaling?.close()
        webrtc?.release()
        _state.value = _state.value.copy(disconnected = true)
    }

    override fun onCleared() {
        statsJob?.cancel()
        signaling?.close()
        webrtc?.release()
    }
}
