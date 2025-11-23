package com.aryanspatel.moodmatch.data.soket.client

import com.aryanspatel.moodmatch.domain.realtime.MoodMatchEvent
import com.aryanspatel.moodmatch.domain.realtime.OutgoingCommand
import kotlinx.coroutines.flow.Flow

// This is the abstraction to which viewModel will talk to.

interface MoodMatchWebSocketClient {

    /** Stream of events from the server. */
    val events: Flow<MoodMatchEvent>

    /** Connection state */
    val connectionState: Flow<ConnectionState>

    suspend fun connect(
        userId: String,
        name: String,
        mood: String,
        avatar: String,
    )

    suspend fun send(command: OutgoingCommand)

    suspend fun disconnect()

    suspend fun retry()
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
