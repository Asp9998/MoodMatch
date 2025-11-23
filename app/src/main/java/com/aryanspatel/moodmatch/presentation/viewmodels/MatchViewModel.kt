package com.aryanspatel.moodmatch.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryanspatel.moodmatch.data.remote.UserSession
import com.aryanspatel.moodmatch.data.repositories.MoodMatchRepository
import com.aryanspatel.moodmatch.data.soket.client.MoodMatchWebSocketClient
import com.aryanspatel.moodmatch.domain.realtime.MoodMatchEvent
import com.aryanspatel.moodmatch.domain.realtime.OutgoingCommand
import com.aryanspatel.moodmatch.presentation.models.MatchUiState
import com.aryanspatel.moodmatch.presentation.models.Message
import com.aryanspatel.moodmatch.presentation.models.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
@HiltViewModel
class MatchViewModel @Inject constructor(
    private val repo: MoodMatchRepository,
    private val wsClient: MoodMatchWebSocketClient,
): ViewModel() {

    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Idle)
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    val sessionState: StateFlow<UserSession?> =
        repo.sessionFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    private var currentRoomId: String? = null
    private var currentUserId: String? = null

    private var lastTypingSent: Boolean = false
    private var typingTimeoutJob: Job? = null

    init {
        observeWsEvents()
        observeConnectionState()
        viewModelScope.launch {
            sessionState
                .filterNotNull()
                .take(1) // only need the first one
                .collect {
                    connectIfNeeded()
                }
        }
    }

    private fun observeWsEvents(){
        viewModelScope.launch {
            try {
                wsClient.events.collect { event ->
                    when (event) {

                        is MoodMatchEvent.MatchFound -> {
                            currentRoomId = event.roomId
                            _uiState.value = MatchUiState.InRoom(
                                roomId = event.roomId,
                                partnerId = event.partnerId,
                                partnerName = event.partnerName,
                                partnerAvatar = event.partnerAvatar,
                                messages = listOf(
                                    Message(
                                        id = UUID.randomUUID().toString(),
                                        type = MessageType.EVENT,
                                        content = "New chat unlocked. One stranger, one shot, no screenshots (hopefully) \uD83D\uDE0F",
                                        timestamp = System.currentTimeMillis()
                                    )
                                ),
                                partnerTyping = false
                            )
                        }

                        is MoodMatchEvent.MessageReceived -> {
                            val userId = currentUserId ?: return@collect
                            val current = _uiState.value

                            if (current is MatchUiState.InRoom &&
                                current.roomId == event.roomId
                            ) {

                                val msg = Message(
                                    id = event.messageId,
                                    type = if (event.fromUSerId == userId) MessageType.SENT else MessageType.RECEIVED,
                                    content = event.text,
                                    timestamp = event.sendAt
                                )

                                _uiState.value = current.copy(
                                    messages = current.messages + msg
                                )
                            }
                        }

                        is MoodMatchEvent.Typing -> {
                            val current = _uiState.value
                            if (current is MatchUiState.InRoom &&
                                current.roomId == event.roomId
                            ) {
                                _uiState.value = current.copy(
                                    partnerTyping = event.isTyping
                                )
                            }
                        }

                        is MoodMatchEvent.PartnerLeft -> {
                            val current = _uiState.value
                            if (current is MatchUiState.InRoom &&
                                current.roomId == event.roomId
                            ) {
                                _uiState.value = MatchUiState.Error(
                                    code = "PARTNER_LEFT",
                                    message = "Your match left the chat. Hit \"start Match\" to find someone with same vide!"
                                )
                            }
                        }

                        MoodMatchEvent.QueueLeft -> {
                            _uiState.value = MatchUiState.Idle
                        }

                        is MoodMatchEvent.Error -> {
                            _uiState.value = MatchUiState.Error(
                                code = event.code,
                                message = event.message
                            )
                        }

                        MoodMatchEvent.HeartbeatAck -> {
                            // handled in wsClient
                            // will automatically timeout
                        }

                        MoodMatchEvent.ConnectionClosed,
                        MoodMatchEvent.ConnectionOpened -> {
                            // show connection status in UI
                        }
                    }
                }
            } catch (e: java.net.SocketException){
                _uiState.value = MatchUiState.Error(
                    code = "PARTNER_LEFT",
                    message = "Your match left the chat. Hit \"start Match\" to find someone with same vide!"
                )
                Log.d("SocketException", "observeWsEvents: $e")
            }
        }
    }

    private fun observeConnectionState(){
        viewModelScope.launch {
            wsClient.connectionState.collect { state ->
                // optionally reflect connection state in UI if needed
            }
        }
    }

    fun connectIfNeeded(){
        viewModelScope.launch {
            val session = repo.sessionFlow().firstOrNull()
            if(session == null){
                _uiState.value = MatchUiState.Error(
                    code = "No_SESSION",
                    message = "User not registered"
                )
                return@launch
            }

            currentUserId = session.userId

            wsClient.connect(
                userId = session.userId,
                name = session.profile.nickname,
                mood = session.profile.mood,
                avatar = session.profile.avatar,
            )
        }
    }

    fun joinQueue(moodLabel: String){
        viewModelScope.launch {
            connectIfNeeded()
            wsClient.send(OutgoingCommand.JoinQueue(moodLabel.lowercase()))
            _uiState.value = MatchUiState.Queueing
        }
    }

    fun leaveQueue() {
        viewModelScope.launch {
            wsClient.send(OutgoingCommand.LeaveQueue)
            _uiState.value = MatchUiState.Idle
        }
    }

    fun leaveChat() {
        val roomId = currentRoomId ?: return
        viewModelScope.launch {
            wsClient.send(OutgoingCommand.SetTyping(roomId, false))
            wsClient.send(OutgoingCommand.LeaveRoom(roomId))
            currentRoomId = null
            _uiState.value = MatchUiState.Idle
        }
    }

    fun sendMessage(text: String){
        val roomId = currentRoomId ?: return
        viewModelScope.launch {
            wsClient.send(OutgoingCommand.SetTyping(roomId, false))
            wsClient.send(OutgoingCommand.SendMessage(roomId, text))
        }
    }

    fun setTyping(isTyping: Boolean){
        val roomId = currentRoomId ?: return
        viewModelScope.launch {
            wsClient.send(OutgoingCommand.SetTyping(roomId, isTyping))
        }
    }

    fun onConsumeMessage(){
        _uiState.value = MatchUiState.Error(null, null)
    }

    fun makeStateIdle(){
        _uiState.value = MatchUiState.Idle
    }

    fun onMessageInputChanged(text: String) {
        val isTypingNow = text.isNotBlank()

        // 1) Edge trigger: user started typing
        if (isTypingNow && !lastTypingSent) {
            lastTypingSent = true
            setTyping(true)
        }

        // 2) User cleared the field
        if (!isTypingNow && lastTypingSent) {
            lastTypingSent = false
            setTyping(false)
        }

//        // 3) Debounce timeout: no input for 3s -> setTyping(false)
        typingTimeoutJob?.cancel()
        if (isTypingNow) {
            typingTimeoutJob = viewModelScope.launch {
                delay(3000)
                if (lastTypingSent) {
                    lastTypingSent = false
                    setTyping(false)
                }
            }
        }
    }

    fun onMessageSent() {
//        typingTimeoutJob?.cancel()
        if (lastTypingSent) {
            lastTypingSent = false
            setTyping(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            wsClient.disconnect()
        }
    }
}