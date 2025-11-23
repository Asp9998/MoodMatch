package com.aryanspatel.moodmatch.domain.realtime


sealed class MoodMatchEvent{

    object ConnectionOpened: MoodMatchEvent()
    object ConnectionClosed: MoodMatchEvent()
    object HeartbeatAck: MoodMatchEvent()

    object QueueLeft: MoodMatchEvent()


    data class MatchFound(
        val roomId: String,
        val mood: String,
        val partnerId: String,
        val partnerName: String?,
        val partnerAvatar: String?
    ): MoodMatchEvent()

    data class MessageReceived(
        val roomId: String,
        val messageId: String,
        val fromUSerId: String,
        val text: String,
        val sendAt: Long
    ): MoodMatchEvent()

    data class Typing(
        val roomId: String,
        val isTyping: Boolean
    ): MoodMatchEvent()

    data class PartnerLeft(
        val roomId: String
    ): MoodMatchEvent()

    data class Error(
        val code: String?,
        val message: String
    ): MoodMatchEvent()
}