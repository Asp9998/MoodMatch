package com.aryanspatel.moodmatch.presentation.models

enum class MessageType { SENT, RECEIVED, EVENT}

data class MoodData(
    val nickname: String,
    val avatar: String,
)

data class Message(
    val id: String,
    val type: MessageType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class MatchUiState {
    object Idle : MatchUiState()
    object Queueing : MatchUiState()

    data class InRoom(
        val roomId: String,
        val partnerId: String,
        val partnerName: String?,
        val partnerAvatar: String?,
        val messages: List<Message>,
        val partnerTyping: Boolean
    ): MatchUiState()

    data class Error(
        val code: String?,
        val message: String?
    ): MatchUiState()
}

data class FloatingEmoji(
    val id: Long,
    val emoji: String,
    val offsetX: Float,
    val startTime: Long
)