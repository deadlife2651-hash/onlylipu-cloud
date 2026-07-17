package com.onlylipu.cloud.streaming

import android.content.Context
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.nio.ByteBuffer

/**
 * Native low-latency WebRTC client for OnlyLipu Cloud.
 * Receives the remote video/audio stream and sends touch/keyboard
 * input back over a reliable DataChannel.
 */
class WebRTCClient(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onLocalOffer(sdp: String)
        fun onLocalIce(candidate: IceCandidate)
        fun onStats(stats: StreamStats)
        fun onStateChanged(state: String)
    }

    val eglBase: EglBase = EglBase.create()
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var inputChannel: DataChannel? = null
    private var renderer: SurfaceViewRenderer? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var lastBytesReceived = 0L
    private var lastStatsTime = 0L

    fun init() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(decoderFactory)   // HW decode on the phone
            .setVideoEncoderFactory(encoderFactory)
            .createPeerConnectionFactory()
    }

    fun attachRenderer(view: SurfaceViewRenderer) {
        renderer = view
        view.init(eglBase.eglBaseContext, null)
        view.setEnableHardwareScaler(true)
        view.setMirror(false)
        remoteVideoTrack?.addSink(view)
    }

    fun connect() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) =
                listener.onLocalIce(candidate)
            override fun onAddStream(stream: MediaStream) {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                val track = receiver.track() ?: return
                when (track) {
                    is VideoTrack -> {
                        remoteVideoTrack = track
                        renderer?.let { track.addSink(it) }
                    }
                    is AudioTrack -> {
                        remoteAudioTrack = track
                        track.setEnabled(true)
                    }
                }
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                listener.onStateChanged(state.name.lowercase())
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        })

        // Reliable ordered channel for touch / key / clipboard events.
        val dcInit = DataChannel.Init().apply { ordered = true }
        inputChannel = peerConnection?.createDataChannel("input", dcInit)

        // Receive-only audio+video.
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() = listener.onLocalOffer(desc.description)
                    override fun onSetFailure(error: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, desc)
            }
            override fun onCreateFailure(error: String?) =
                listener.onStateChanged("offer_failed")
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun onRemoteAnswer(sdp: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, desc)
    }

    fun addRemoteIce(sdpMid: String?, mLineIndex: Int, candidate: String) {
        peerConnection?.addIceCandidate(IceCandidate(sdpMid, mLineIndex, candidate))
    }

    /** Send one input event as compact JSON over the input DataChannel. */
    fun sendInput(event: String) {
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(event.toByteArray()), false)
        inputChannel?.send(buffer)
    }

    fun setAudioEnabled(enabled: Boolean) {
        remoteAudioTrack?.setEnabled(enabled)
    }

    fun pollStats() {
        val pc = peerConnection ?: return
        val now = System.currentTimeMillis()
        pc.getStats { report ->
            var rttMs = 0
            var fps = 0
            var bytes = 0L
            var loss = 0.0
            var width = 0; var height = 0
            for (s in report.statsMap.values) {
                when (s.type) {
                    "candidate-pair" -> {
                        val rtt = s.members["currentRoundTripTime"] as? Double
                        if (rtt != null) rttMs = (rtt * 1000).toInt()
                    }
                    "inbound-rtp" -> {
                        if (s.members["kind"] == "video") {
                            fps = (s.members["framesPerSecond"] as? Double)?.toInt() ?: fps
                            bytes = (s.members["bytesReceived"] as? Long) ?: bytes
                            val lost = (s.members["packetsLost"] as? Long) ?: 0L
                            val received = (s.members["packetsReceived"] as? Long) ?: 1L
                            loss = lost.toDouble() / (lost + received).coerceAtLeast(1) * 100.0
                            width = (s.members["frameWidth"] as? Long)?.toInt() ?: width
                            height = (s.members["frameHeight"] as? Long)?.toInt() ?: height
                        }
                    }
                }
            }
            val dt = (now - lastStatsTime).coerceAtLeast(1) / 1000.0
            val kbps = if (lastStatsTime > 0) ((bytes - lastBytesReceived) * 8 / dt / 1000).toInt() else 0
            lastBytesReceived = bytes
            lastStatsTime = now
            listener.onStats(
                StreamStats(
                    pingMs = rttMs, fps = fps, bitrateKbps = kbps.coerceAtLeast(0),
                    packetLossPct = loss,
                    resolution = if (width > 0) "${width}x${height}" else "-",
                    state = "connected"
                )
            )
        }
    }

    fun release() {
        remoteVideoTrack?.removeSink(renderer)
        renderer?.release()
        inputChannel?.close()
        peerConnection?.close()
        peerConnection?.dispose()
        factory?.dispose()
        renderer = null
        peerConnection = null
        factory = null
    }
}
