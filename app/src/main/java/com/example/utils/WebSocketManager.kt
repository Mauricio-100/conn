package com.example.utils

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class SocketConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}

interface ChatSocketClient {
    val connectionState: StateFlow<SocketConnectionState>
    val events: SharedFlow<WebSocketEvent>
    fun connect(userId: String)
    fun disconnect()
    fun sendVoiceMessage(receiverId: String, audioB64: String, senderUsername: String): Boolean
    fun sendTypingStatus(receiverId: String, isTyping: Boolean): Boolean
}

object WebSocketManager : ChatSocketClient {
    private const val TAG = "WebSocketManager"
    private val client = OkHttpClient.Builder()
        .pingInterval(0, TimeUnit.SECONDS) // Handle manually
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 100)
    override val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private var currentUserId: String? = null
    private var isClosedManually = false
    private var reconnectJob: Job? = null
    private var reconnectDelayMs = 1000L
    private val maxReconnectDelayMs = 30000L
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun connect(userId: String) {
        if (currentUserId == userId && webSocket != null) {
            Log.d(TAG, "Already connected/connecting for user: $userId")
            return
        }

        disconnect() // Reset previous

        currentUserId = userId
        isClosedManually = false
        reconnectDelayMs = 1000L
        _connectionState.value = SocketConnectionState.CONNECTING

        val url = "wss://hoosthubs-g.onrender.com/ws/$userId"
        val request = Request.Builder().url(url).build()

        Log.d(TAG, "Connecting to WebSocket: $url")
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Opened successfully")
                reconnectDelayMs = 1000L
                _connectionState.value = SocketConnectionState.CONNECTED
                coroutineScope.launch {
                    _events.emit(WebSocketEvent.Connected)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: code=$code, reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed: code=$code, reason=$reason")
                _connectionState.value = SocketConnectionState.DISCONNECTED
                coroutineScope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
                triggerAutoReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure", t)
                _connectionState.value = SocketConnectionState.DISCONNECTED
                coroutineScope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
                triggerAutoReconnect()
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "ping" -> {
                    sendPong()
                }
                "new_message" -> {
                    val rawId = json.optString("message_id")
                    val finalId = if (rawId.isNullOrBlank()) json.optString("id") else rawId
                    val event = WebSocketEvent.NewMessage(
                        messageId = finalId,
                        senderId = json.optString("sender_id"),
                        content = json.optString("content"),
                        msgType = json.optString("msg_type"),
                        senderUsername = json.optString("sender_username"),
                        timestamp = json.optString("timestamp")
                    )
                    coroutineScope.launch { _events.emit(event) }
                }
                "message_sent" -> {
                    val rawId = json.optString("message_id")
                    val finalId = if (rawId.isNullOrBlank()) json.optString("id") else rawId
                    val event = WebSocketEvent.MessageSent(
                        messageId = finalId,
                        content = json.optString("content"),
                        msgType = json.optString("msg_type")
                    )
                    coroutineScope.launch { _events.emit(event) }
                }
                "message_deleted" -> {
                    val rawId = json.optString("message_id")
                    val finalId = if (rawId.isNullOrBlank()) json.optString("id") else rawId
                    if (finalId.isNotBlank()) {
                        val event = WebSocketEvent.MessageDeleted(finalId)
                        coroutineScope.launch { _events.emit(event) }
                    }
                }
                "typing_status" -> {
                    val senderId = json.optString("sender_id")
                    val isTyping = json.optBoolean("is_typing", false)
                    if (senderId.isNotBlank()) {
                        coroutineScope.launch { _events.emit(WebSocketEvent.TypingStatus(senderId, isTyping)) }
                    }
                }
                "error" -> {
                    val msg = json.optString("message", "Erreur serveur")
                    coroutineScope.launch { _events.emit(WebSocketEvent.Error(msg)) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming WS message: $text", e)
        }
    }

    private fun sendPong() {
        try {
            val pong = JSONObject().apply {
                put("type", "pong")
            }
            webSocket?.send(pong.toString())
            Log.d(TAG, "Sent PONG")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending PONG", e)
        }
    }

    override fun sendVoiceMessage(receiverId: String, audioB64: String, senderUsername: String): Boolean {
        val socket = webSocket
        if (socket == null) {
            Log.e(TAG, "Cannot send message, WebSocket is not connected")
            return false
        }

        return try {
            val payload = JSONObject().apply {
                put("type", "voice_message")
                put("receiver_id", receiverId)
                put("audio_data", audioB64)
                put("sender_username", senderUsername)
            }
            val success = socket.send(payload.toString())
            Log.d(TAG, "Sent voice message payload to WS, status=$success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send voice message", e)
            false
        }
    }

    override fun sendTypingStatus(receiverId: String, isTyping: Boolean): Boolean {
        val socket = webSocket
        if (socket == null) return false
        return try {
            val payload = JSONObject().apply {
                put("type", "typing_status")
                put("receiver_id", receiverId)
                put("is_typing", isTyping)
            }
            socket.send(payload.toString())
        } catch (e: Exception) {
            false
        }
    }

    private fun triggerAutoReconnect() {
        if (isClosedManually || currentUserId == null) return

        reconnectJob?.cancel()
        _connectionState.value = SocketConnectionState.CONNECTING
        reconnectJob = coroutineScope.launch {
            Log.d(TAG, "Waiting $reconnectDelayMs ms before reconnecting...")
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(maxReconnectDelayMs)
            currentUserId?.let { connect(it) }
        }
    }

    override fun disconnect() {
        isClosedManually = true
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "Disconnect requested")
        } catch (e: Exception) {
            // ignore
        }
        webSocket = null
        currentUserId = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }
}

sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class NewMessage(
        val messageId: String,
        val senderId: String,
        val content: String,
        val msgType: String,
        val senderUsername: String,
        val timestamp: String
    ) : WebSocketEvent()
    data class MessageSent(
        val messageId: String,
        val content: String,
        val msgType: String
    ) : WebSocketEvent()
    data class MessageDeleted(
        val messageId: String
    ) : WebSocketEvent()
    data class TypingStatus(
        val senderId: String,
        val isTyping: Boolean
    ) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
}
