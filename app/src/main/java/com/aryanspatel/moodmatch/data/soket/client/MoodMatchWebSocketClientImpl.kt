package com.aryanspatel.moodmatch.data.soket.client

import android.util.Log
import com.aryanspatel.moodmatch.data.remote.ApiConfig
import com.aryanspatel.moodmatch.data.soket.model.WsEnvelopeDto
import com.aryanspatel.moodmatch.di.ApplicationScope
import com.aryanspatel.moodmatch.domain.realtime.MoodMatchEvent
import com.aryanspatel.moodmatch.domain.realtime.OutgoingCommand
import com.aryanspatel.moodmatch.domain.usecases.AuthTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class MoodMatchWebSocketClientImpl @Inject constructor(
    private val client: HttpClient,
    private val parser: WebSocketEventParser,
    private val json: Json,
    private val authTokenProvider: AuthTokenProvider,
    @ApplicationScope private val scope: CoroutineScope,
): MoodMatchWebSocketClient {

    private val _events = MutableSharedFlow<MoodMatchEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    private var sessionJob: Job? = null
    private var heartbeatJob: Job? = null
    @Volatile
    private var lastHeartbeatAckAt: Long = 0
//    private val HEARTBEAT_INTERVAL_MS = 25_000L   // send heartbeat every 25s
    private val HEARTBEAT_TIMEOUT_MS = 60_000L    // if no ack for 60s => dead

    private val MAX_RECONNECT_ATTEMPTS = 5

    @Volatile
    private var manualDisconnectRequested: Boolean = false

    private val wsUrl = ApiConfig.wsUrl
    private var activeSession: DefaultClientWebSocketSession? = null

    override suspend fun connect(
        userId: String,
        name: String,
        mood: String,
        avatar: String,
    ) {

        if(_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING)
            return

        manualDisconnectRequested = false
        _connectionState.value = ConnectionState.CONNECTING

        sessionJob?.cancel()
        sessionJob = scope.launch {
            reconnectLoop()
        }
    }

    private suspend fun reconnectLoop(){
        var attempt = 0

        while(scope.isActive && !manualDisconnectRequested) {
            if(attempt >= MAX_RECONNECT_ATTEMPTS){
                // Give up auto-reconnect
                _connectionState.value = ConnectionState.DISCONNECTED
                _events.emit(
                    MoodMatchEvent.Error(
                        code = "RECONNECT_EXHAUSTED",
                        message = "Unable to reconnect. Please try again."
                    )
                )
                break
            }

            val token = authTokenProvider.currentToken()
            if(token.isNullOrBlank()){
                _connectionState.value = ConnectionState.DISCONNECTED
                _events.emit(
                    MoodMatchEvent.Error(
                        code = "NO_TOKEN",
                        message = "Missing auth token. Please sign in again."
                    )
                )
                break
            }

            try{
                _connectionState.value =
                    if (attempt == 0) ConnectionState.CONNECTING else ConnectionState.RECONNECTING

                client.webSocket(
                    urlString = wsUrl,
                    request = {
                        header("Authorization", "Bearer $token")
                    }
                ){
                    activeSession = this // Store the session
                    lastHeartbeatAckAt = System.currentTimeMillis()

                    _connectionState.value = ConnectionState.CONNECTED
                    _events.emit(MoodMatchEvent.ConnectionOpened)

                    // start heartbeat loop
                    heartbeatJob?.cancel()
                    heartbeatJob = scope.launch {
                        sendHeartbeatLoop()
                    }

                    // read loop
                    incoming.consumeEach { frame ->
                        if(frame is Frame.Text){
                            val text = frame.readText()
                            val event = parser.parse(text)
                            if(event != null){
                                // update heartbeat time on ack
                                if(event is MoodMatchEvent.HeartbeatAck){
                                    lastHeartbeatAckAt = System.currentTimeMillis()
                                }
                                _events.emit(event)
                            }
                        }
                    }

                    // if we exit consumeEach, connection is closed
                    activeSession = null
                    _events.emit(MoodMatchEvent.ConnectionClosed)

                }
            } catch (e: Exception){
                Log.d("ExceptionLog", "reconnectLoop: WS error: $e")
            }

            if (manualDisconnectRequested){
                // user intentionally disconnected; don't auto-reconnect
                break
            }

            _connectionState.value = ConnectionState.DISCONNECTED

            // Stop heartbeat when disconnected
            heartbeatJob?.cancel()
            heartbeatJob = null

            // Backoff before reconnect
            attempt++
            val delayMs = (1000L * attempt).coerceAtMost(10_000L)
            delay(delayMs)
        }
    }

    private suspend fun sendHeartbeatLoop(){
        while(scope.isActive && _connectionState.value == ConnectionState.CONNECTED){
            val now = System.currentTimeMillis()
            val sinceLastAck = now - lastHeartbeatAckAt

            // If too long since last ack -> connection probably dead
            if(sinceLastAck > HEARTBEAT_TIMEOUT_MS){
                // option 1: emit an event for UI, then close
                _events.emit(
                    MoodMatchEvent.Error(
                        code = "HEARTBEAT_TIMEOUT",
                        message = "Connection lost (heartbeat timeout)."
                    )
                )

                // Close the socket; reconnectLoop will see closure and retry
                activeSession?.close(
                    CloseReason(
                        CloseReason.Codes.NORMAL,
                        "Heartbeat timeout"
                    )
                )

                activeSession = null
                _connectionState.value = ConnectionState.DISCONNECTED
                break
            }

            send(OutgoingCommand.SendHeartbeat)
            delay(25_000L)
        }
    }



    override suspend fun send(command: OutgoingCommand) {
        try {


            // just send JSON envelope to server
            val envelope = when (command) {
                is OutgoingCommand.SendMessage -> WsEnvelopeDto(
                    type = "send_message",
                    payload = buildJsonObject {
                        put("roomId", command.roomId)
                        put("text", command.text)
                    }
                )

                is OutgoingCommand.SetTyping -> WsEnvelopeDto(
                    type = "typing",
                    payload = buildJsonObject {
                        put("roomId", command.roomId)
                        put("isTyping", command.isTyping)
                    }
                )

                is OutgoingCommand.SendHeartbeat -> WsEnvelopeDto(
                    type = "heartbeat",
                    payload = null
                )

                is OutgoingCommand.JoinQueue -> WsEnvelopeDto(
                    type = "join_queue",
                    payload = buildJsonObject {
                        put("mood", command.moodLabel)
                    }
                )

                OutgoingCommand.LeaveQueue -> WsEnvelopeDto(
                    type = "leave_queue",
                    payload = null
                )

                is OutgoingCommand.LeaveRoom -> WsEnvelopeDto(
                    type = "leave_room",
                    payload = buildJsonObject {
                        put("roomId", command.roomId)
                    }
                )
            }

            val text = json.encodeToString(envelope)
            val session = activeSession
            if (session != null) {
                session.send(Frame.Text(text))
            } else {
                Log.d("ExceptionLog", "send: not connected, cannot send")
            }
        } catch (e: CancellationException) {
            Log.d("SendCommandError", "send: CancellationException: $e")
        } catch (e: java.net.SocketException) {
//            onDisconnected(e)
        } catch (e: Throwable) {
            Log.d("SendCommandError", "send: Error while send Command: $e")
        }
    }

    override suspend fun disconnect() {
        manualDisconnectRequested = true

        heartbeatJob?.cancel()
        heartbeatJob = null

        activeSession?.close(
            CloseReason(
                CloseReason.Codes.NORMAL,
                "Manual disconnect"
            )
        )

        sessionJob?.cancel()
        sessionJob = null

        _connectionState.value = ConnectionState.DISCONNECTED
        _events.emit(MoodMatchEvent.ConnectionClosed)
    }

    override suspend fun retry() {
        manualDisconnectRequested = false
        sessionJob?.cancel()  // clear any old loop
        sessionJob = scope.launch {
            reconnectLoop()
        }
    }

}