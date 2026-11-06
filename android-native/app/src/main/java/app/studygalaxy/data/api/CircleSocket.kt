package app.studygalaxy.data.api

import app.studygalaxy.BuildConfig
import app.studygalaxy.data.model.CircleEventFrame
import app.studygalaxy.data.model.FocusingMember
import app.studygalaxy.data.model.PresenceFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** What the Circles screen renders. */
data class CirclePresenceState(
    val connected: Boolean = false,
    val focusing: List<FocusingMember> = emptyList(),
    val online: List<String> = emptyList(),
    val feed: List<String> = emptyList()
)

/**
 * Holds a WebSocket open against /ws/circles/{id} and keeps a presence snapshot
 * in a StateFlow. Reconnects with backoff, and pings so mobile NATs do not drop
 * an idle socket mid-session.
 */
class CircleSocket(
    private val scope: CoroutineScope,
    private val circleId: String,
    private val token: String
) {
    private val _state = MutableStateFlow(CirclePresenceState())
    val state: StateFlow<CirclePresenceState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var socket: WebSocket? = null
    private var loop: Job? = null
    private var closedByUs = false

    private fun wsUrl(): String {
        val base = BuildConfig.API_BASE
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        return "$base/ws/circles/$circleId?token=$token"
    }

    fun connect() {
        if (loop != null) return
        closedByUs = false
        loop = scope.launch {
            var attempt = 0
            while (isActive && !closedByUs) {
                val opened = openOnce()
                if (closedByUs) break
                // Backoff 2s, 4s, 8s … capped at 30s so a sleeping free-tier
                // server does not get hammered.
                attempt = if (opened) 0 else (attempt + 1).coerceAtMost(4)
                delay(if (attempt == 0) 2_000L else (2_000L shl attempt).coerceAtMost(30_000L))
            }
        }
    }

    /** Opens a socket and suspends until it closes or fails. */
    private suspend fun openOnce(): Boolean {
        var everOpen = false
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        val req = Request.Builder().url(wsUrl()).build()

        socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                everOpen = true
                _state.value = _state.value.copy(connected = true)
                webSocket.send("""{"type":"presence?"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) = handle(text)

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = _state.value.copy(connected = false)
                if (!done.isCompleted) done.complete(Unit)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = _state.value.copy(connected = false)
                if (!done.isCompleted) done.complete(Unit)
            }
        })

        done.await()
        return everOpen
    }

    private fun handle(text: String) {
        val type = runCatching {
            json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.content
        }.getOrNull() ?: return

        when (type) {
            "pong" -> Unit
            "presence" -> {
                val f = runCatching { json.decodeFromString(PresenceFrame.serializer(), text) }.getOrNull()
                    ?: return
                _state.value = _state.value.copy(focusing = f.focusing, online = f.online)
            }
            else -> {
                val e = runCatching { json.decodeFromString(CircleEventFrame.serializer(), text) }.getOrNull()
                    ?: return
                val who = e.cosmicName ?: "Someone"
                val line = when (e.type) {
                    "session_start" ->
                        "$who started a ${e.plannedMin ?: 0}-min session" +
                            (e.intention?.takeIf { it.isNotBlank() }?.let { " — \"$it\"" } ?: "")
                    "session_complete" -> "$who finished ${e.actualMin ?: 0} min ✨"
                    "session_broken" -> "$who broke a session — black hole 🕳"
                    else -> return
                }
                _state.value = _state.value.copy(feed = (listOf(line) + _state.value.feed).take(20))
            }
        }
    }

    fun close() {
        closedByUs = true
        runCatching { socket?.close(1000, "bye") }
        socket = null
        loop?.cancel()
        loop = null
        _state.value = _state.value.copy(connected = false)
    }
}
