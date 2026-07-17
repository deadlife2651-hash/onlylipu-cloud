package com.onlylipu.cloud.streaming

import com.onlylipu.cloud.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * Secure WebSocket signaling channel used to exchange WebRTC SDP/ICE
 * with the OnlyLipu server gateway. Reconnects automatically.
 */
class SignalingClient(
    private val token: String,
    private val listener: Listener
) {
    interface Listener {
        fun onAnswer(sdp: String)
        fun onIceCandidate(sdpMid: String?, sdpMLineIndex: Int, candidate: String)
        fun onServerEvent(event: String, payload: JSONObject)
        fun onClosed(reason: String)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()
    private var ws: WebSocket? = null
    private var closedByUser = false
    private var retry = 0

    fun connect(environment: String) {
        closedByUser = false
        val request = Request.Builder()
            .url(ApiClient.wsSignalingUrl() + "?env=" + environment)
            .header("Authorization", "Bearer $token")
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retry = 0
                send(JSONObject().put("type", "hello").put("env", environment))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (msg.optString("type")) {
                    "answer" -> listener.onAnswer(msg.getString("sdp"))
                    "ice" -> listener.onIceCandidate(
                        msg.optString("sdpMid"),
                        msg.optInt("sdpMLineIndex"),
                        msg.optString("candidate")
                    )
                    else -> listener.onServerEvent(msg.optString("type"), msg)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scheduleReconnect(environment, "Connection failed: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!closedByUser) scheduleReconnect(environment, reason)
                else listener.onClosed(reason)
            }
        })
    }

    private fun scheduleReconnect(environment: String, reason: String) {
        if (closedByUser) {
            listener.onClosed(reason)
            return
        }
        retry++
        if (retry > 5) {
            listener.onClosed("Server unreachable after $retry attempts")
            return
        }
        scope.launch {
            delay((retry * 1500L).coerceAtMost(8000L))
            connect(environment)
        }
    }

    fun sendOffer(sdp: String) =
        send(JSONObject().put("type", "offer").put("sdp", sdp))

    fun sendIce(sdpMid: String?, mLineIndex: Int, candidate: String) =
        send(JSONObject().put("type", "ice")
            .put("sdpMid", sdpMid)
            .put("sdpMLineIndex", mLineIndex)
            .put("candidate", candidate))

    fun sendQuality(maxHeight: Int, maxFps: Int, maxBitrateKbps: Int) =
        send(JSONObject().put("type", "quality")
            .put("maxHeight", maxHeight)
            .put("maxFps", maxFps)
            .put("maxBitrateKbps", maxBitrateKbps))

    private fun send(obj: JSONObject) {
        ws?.send(obj.toString())
    }

    fun close() {
        closedByUser = true
        ws?.close(1000, "client disconnect")
        client.dispatcher.executorService.shutdown()
    }
}
