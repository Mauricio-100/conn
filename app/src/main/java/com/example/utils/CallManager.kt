package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.IddetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

sealed class CallState {
    object Idle : CallState()

    data class OutgoingRinging(
        val callId: String,
        val calleeId: String,
        val calleeUsername: String,
        val calleeAvatar: String?
    ) : CallState()

    data class IncomingRinging(
        val callId: String,
        val callerId: String,
        val callerUsername: String,
        val callerAvatar: String?
    ) : CallState()

    data class Active(
        val callId: String,
        val peerId: String,
        val peerUsername: String,
        val peerAvatar: String?,
        val startedAt: Long = System.currentTimeMillis(),
        val durationSeconds: Int = 0,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = true,
        val isConnecting: Boolean = true
    ) : CallState()

    data class Ended(
        val callId: String,
        val peerUsername: String,
        val durationSeconds: Int = 0,
        val reason: String = "Appel terminé"
    ) : CallState()
}

object CallManager {
    private const val TAG = "CallManager"
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val BUFFER_SIZE = 1280 // 40ms of 16kHz 16-bit mono

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null
    private var vibrationJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    private var audioWebSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websocket stream
        .build()

    private var repository: IddetRepository? = null
    private var currentUserId: String? = null
    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var listenerJob: Job? = null

    fun initialize(context: Context, repo: IddetRepository) {
        this.appContext = context.applicationContext
        this.repository = repo
        this.audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        this.vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Listen for WebSocket Call events once
        if (listenerJob == null || listenerJob?.isCancelled == true) {
            listenerJob = scope.launch {
                WebSocketManager.events.collect { event ->
                    when (event) {
                        is WebSocketEvent.CallInvite -> {
                            handleIncomingCall(event.callId, event.callerId, event.callerUsername, event.callerAvatar)
                        }
                        is WebSocketEvent.CallAccepted -> {
                            handleCallAccepted(event.callId, event.calleeId)
                        }
                        is WebSocketEvent.CallDeclined -> {
                            handleCallDeclined(event.callId)
                        }
                        is WebSocketEvent.CallUnavailable -> {
                            handleCallUnavailable()
                        }
                        is WebSocketEvent.CallEnded -> {
                            handleCallEnded(event.callId, event.durationSeconds)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun setCurrentUser(userId: String?) {
        this.currentUserId = userId
    }

    /**
     * Start an outgoing call to another user
     */
    fun startCall(calleeId: String, calleeUsername: String, calleeAvatar: String?, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val repo = repository ?: run {
            CoroutineScope(Dispatchers.Main).launch {
                onResult(false, "Service d'appel non initialisé")
            }
            return
        }

        if (_callState.value !is CallState.Idle) {
            CoroutineScope(Dispatchers.Main).launch {
                onResult(false, "Un appel est déjà en cours")
            }
            return
        }

        _callState.value = CallState.OutgoingRinging(
            callId = "",
            calleeId = calleeId,
            calleeUsername = calleeUsername,
            calleeAvatar = calleeAvatar
        )
        startOutgoingRingTone()

        scope.launch {
            val res = repo.startCall(calleeId)
            res.fold(
                onSuccess = { response ->
                    Log.i(TAG, "Call initiated successfully: ${response.call_id}")
                    val current = _callState.value
                    if (current is CallState.OutgoingRinging) {
                        _callState.value = current.copy(callId = response.call_id)
                    }
                    withContext(Dispatchers.Main) {
                        onResult(true, null)
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Call could not be started: ${error.message}")
                    stopRingTone()
                    val reason = error.message ?: "Impossible de joindre @$calleeUsername"
                    _callState.value = CallState.Ended(
                        callId = "",
                        peerUsername = calleeUsername,
                        reason = reason
                    )
                    withContext(Dispatchers.Main) {
                        onResult(false, reason)
                    }
                    autoResetToIdleAfterDelay()
                }
            )
        }
    }

    /**
     * Accept an incoming call
     */
    fun acceptCall(callId: String) {
        val current = _callState.value
        if (current !is CallState.IncomingRinging) return

        stopRingTone()
        val repo = repository ?: return

        _callState.value = CallState.Active(
            callId = callId,
            peerId = current.callerId,
            peerUsername = current.callerUsername,
            peerAvatar = current.callerAvatar,
            isConnecting = true
        )

        scope.launch {
            repo.acceptCall(callId)
            startAudioStream(callId, current.callerId)
        }
    }

    /**
     * Decline an incoming call
     */
    fun declineCall(callId: String) {
        stopRingTone()
        val repo = repository
        val current = _callState.value

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = (current as? CallState.IncomingRinging)?.callerUsername ?: "Contact",
            reason = "Appel refusé"
        )

        scope.launch {
            repo?.declineCall(callId)
        }
        autoResetToIdleAfterDelay()
    }

    /**
     * End active or outgoing call
     */
    fun endCall() {
        stopRingTone()
        val current = _callState.value
        val repo = repository

        val callId = when (current) {
            is CallState.Active -> current.callId
            is CallState.OutgoingRinging -> current.callId
            is CallState.IncomingRinging -> current.callId
            else -> ""
        }
        val peerName = when (current) {
            is CallState.Active -> current.peerUsername
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.IncomingRinging -> current.callerUsername
            else -> "Contact"
        }
        val duration = (current as? CallState.Active)?.durationSeconds ?: 0

        stopAudioStream()

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            durationSeconds = duration,
            reason = "Appel terminé"
        )

        if (callId.isNotBlank()) {
            scope.launch {
                repo?.endCall(callId)
            }
        }
        autoResetToIdleAfterDelay()
    }

    /**
     * Toggle microphone mute state
     */
    fun toggleMute() {
        val current = _callState.value
        if (current is CallState.Active) {
            val newMute = !current.isMuted
            _callState.value = current.copy(isMuted = newMute)
        }
    }

    /**
     * Toggle speakerphone on/off
     */
    fun toggleSpeakerphone() {
        val current = _callState.value
        if (current is CallState.Active) {
            val newSpeaker = !current.isSpeakerOn
            audioManager?.isSpeakerphoneOn = newSpeaker
            _callState.value = current.copy(isSpeakerOn = newSpeaker)
        }
    }

    // Handle WebSocket server notifications
    private fun handleIncomingCall(callId: String, callerId: String, callerUsername: String, callerAvatar: String?) {
        if (_callState.value !is CallState.Idle) {
            // Already busy
            scope.launch { repository?.declineCall(callId) }
            return
        }

        _callState.value = CallState.IncomingRinging(
            callId = callId,
            callerId = callerId,
            callerUsername = callerUsername,
            callerAvatar = callerAvatar
        )
        
        appContext?.let { ctx ->
            CallRingtonePlayer.startRinging(ctx)
            scope.launch {
                NotificationHelper.showIncomingCallNotification(
                    context = ctx,
                    callId = callId,
                    callerId = callerId,
                    callerUsername = callerUsername,
                    callerAvatar = callerAvatar
                )
            }
        }
    }

    /**
     * Decline incoming call with an automated Quick Reply message (WhatsApp style)
     */
    fun declineWithQuickReply(
        context: Context?,
        callId: String,
        callerId: String,
        quickMessage: String,
        callerUsername: String = "Contact"
    ) {
        stopRingTone()
        stopAudioStream()
        val ctx = context ?: appContext
        ctx?.let { NotificationHelper.cancelIncomingCallNotification(it) }

        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = callerUsername,
            reason = "Message envoyé : \"$quickMessage\""
        )

        scope.launch {
            try {
                repository?.declineCall(callId)
            } catch (_: Exception) {}
            try {
                repository?.sendMessage(callerId, quickMessage, "text")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send quick reply message: ${e.message}")
            }
        }
        autoResetToIdleAfterDelay()
    }

    private fun handleCallAccepted(callId: String, calleeId: String) {
        val current = _callState.value
        if (current is CallState.OutgoingRinging) {
            stopRingTone()
            _callState.value = CallState.Active(
                callId = callId,
                peerId = calleeId,
                peerUsername = current.calleeUsername,
                peerAvatar = current.calleeAvatar,
                isConnecting = true
            )
            startAudioStream(callId, calleeId)
        }
    }

    private fun handleCallDeclined(callId: String) {
        stopRingTone()
        stopAudioStream()
        val current = _callState.value
        val peerName = when (current) {
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.Active -> current.peerUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            reason = "Appel refusé par le correspondant"
        )
        autoResetToIdleAfterDelay()
    }

    private fun handleCallUnavailable() {
        stopRingTone()
        stopAudioStream()
        val current = _callState.value
        val peerName = when (current) {
            is CallState.OutgoingRinging -> current.calleeUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = "",
            peerUsername = peerName,
            reason = "@$peerName n'est pas en ligne"
        )
        autoResetToIdleAfterDelay()
    }

    private fun handleCallEnded(callId: String, durationSeconds: Int) {
        stopRingTone()
        stopAudioStream()
        val current = _callState.value
        val peerName = when (current) {
            is CallState.Active -> current.peerUsername
            is CallState.OutgoingRinging -> current.calleeUsername
            is CallState.IncomingRinging -> current.callerUsername
            else -> "Contact"
        }
        _callState.value = CallState.Ended(
            callId = callId,
            peerUsername = peerName,
            durationSeconds = durationSeconds,
            reason = "Appel terminé"
        )
        autoResetToIdleAfterDelay()
    }

    private fun startAudioStream(callId: String, peerId: String) {
        val userId = currentUserId ?: repository?.currentUser?.value?.id ?: return
        val wsUrl = "wss://hoosthubs-g.onrender.com/ws/call-audio/$callId/$userId"

        Log.i(TAG, "Connecting to audio relay WebSocket: $wsUrl")
        val request = Request.Builder().url(wsUrl).build()

        audioWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Audio relay WebSocket opened successfully")
                scope.launch {
                    val current = _callState.value
                    if (current is CallState.Active) {
                        _callState.value = current.copy(isConnecting = false)
                    }
                    startHardwareAudio(webSocket)
                    startDurationTimer()
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Received remote audio chunk -> Play via AudioTrack
                playAudioChunk(bytes.toByteArray())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Audio relay WebSocket error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Audio relay WebSocket closed: $code / $reason")
            }
        })
    }

    private fun startHardwareAudio(webSocket: WebSocket) {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true

            // Setup AudioTrack for playback
            val minPlayBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(minPlayBufSize.coerceAtLeast(BUFFER_SIZE * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true

            // Setup AudioRecord for microphone capture
            val minRecBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                minRecBufSize.coerceAtLeast(BUFFER_SIZE * 4)
            )

            audioRecord?.startRecording()
            isRecording = true

            // Capture loop
            scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(BUFFER_SIZE)
                while (isRecording && isActive) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    if (read > 0) {
                        val isMuted = (_callState.value as? CallState.Active)?.isMuted == true
                        if (!isMuted) {
                            webSocket.send(buffer.copyOf(read).toByteString())
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission not granted for audio call", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting hardware audio", e)
        }
    }

    private fun playAudioChunk(data: ByteArray) {
        try {
            if (isPlaying && audioTrack != null) {
                audioTrack?.write(data, 0, data.size)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error playing audio chunk: ${e.message}")
        }
    }

    private fun stopAudioStream() {
        isRecording = false
        isPlaying = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        try {
            audioWebSocket?.close(1000, "Call ended")
        } catch (_: Exception) {}
        audioWebSocket = null

        timerJob?.cancel()
        timerJob = null

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (isActive) {
                delay(1000)
                seconds++
                val current = _callState.value
                if (current is CallState.Active) {
                    _callState.value = current.copy(durationSeconds = seconds)
                } else {
                    break
                }
            }
        }
    }

    private fun startIncomingRinging() {
        vibrationJob?.cancel()
        vibrationJob = scope.launch {
            while (isActive && _callState.value is CallState.IncomingRinging) {
                vibrator?.let { v ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 1000), 0))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(longArrayOf(0, 800, 1000), 0)
                    }
                }
                delay(2000)
            }
        }
    }

    private fun startOutgoingRingTone() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            scope.launch {
                while (isActive && _callState.value is CallState.OutgoingRinging) {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1000)
                    delay(3500)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator error: ${e.message}")
        }
    }

    private fun stopRingTone() {
        CallRingtonePlayer.stopRinging()
        appContext?.let { NotificationHelper.cancelIncomingCallNotification(it) }
        vibrationJob?.cancel()
        vibrationJob = null
        vibrator?.cancel()
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }

    fun resetToIdle() {
        stopRingTone()
        stopAudioStream()
        _callState.value = CallState.Idle
    }

    private fun autoResetToIdleAfterDelay() {
        scope.launch {
            delay(2800)
            if (_callState.value is CallState.Ended) {
                _callState.value = CallState.Idle
            }
        }
    }
}
