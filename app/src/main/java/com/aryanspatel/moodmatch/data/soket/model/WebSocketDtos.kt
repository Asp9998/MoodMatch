package com.aryanspatel.moodmatch.data.soket.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import javax.annotation.meta.TypeQualifierNickname

// Small parser that converts Json payload into -> MoodMatchEvent

// Envelop and payload DTOs

@Serializable
data class WsEnvelopeDto(
    val type: String,
    val payload: JsonElement? = null
)

@Serializable
data class MatchFoundDto(
    val roomId: String,
    val mood: String,
    val partnerId: String,
    val partnerNickname: String? = null,
    val partnerAvatar: String? = null
)

@Serializable
data class MessageDto(
    val randomId: String,
    val senderId: String,
    val text: String,
    val messageId: String,
    val ts: Long
)

@Serializable
data class TypingDto(
    val roomId: String,
    val isTyping: Boolean
)

@Serializable
data class PartnerLeftDto(
    val roomId: String
)

@Serializable
data class QueueLeftDto(
    val dummy: String = "" // no need to send any payload
)


@Serializable
data class ErrorDto(
    val code: String? = null,
    val message: String
)
