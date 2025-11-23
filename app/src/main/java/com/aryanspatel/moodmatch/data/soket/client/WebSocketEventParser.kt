package com.aryanspatel.moodmatch.data.soket.client

import android.util.Log
import com.aryanspatel.moodmatch.data.soket.model.ErrorDto
import com.aryanspatel.moodmatch.data.soket.model.MatchFoundDto
import com.aryanspatel.moodmatch.data.soket.model.MessageDto
import com.aryanspatel.moodmatch.data.soket.model.PartnerLeftDto
import com.aryanspatel.moodmatch.data.soket.model.TypingDto
import com.aryanspatel.moodmatch.data.soket.model.WsEnvelopeDto
import com.aryanspatel.moodmatch.domain.realtime.MoodMatchEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.lang.Exception

class WebSocketEventParser(
    private val json: Json
){
    fun parse(raw: String): MoodMatchEvent? {
        return try{
            val env = json.decodeFromString<WsEnvelopeDto>(raw)
            when(env.type){
                "match_found" -> {
                    val dto = env.payload?.let {
                        json.decodeFromJsonElement<MatchFoundDto>(it)
                    } ?: return null

                    MoodMatchEvent.MatchFound(
                        roomId = dto.roomId,
                        mood = dto.mood,
                        partnerId = dto.partnerId,
                        partnerName = dto.partnerNickname,
                        partnerAvatar = dto.partnerAvatar
                    )
                }

                "message" -> {
                    val dto = env.payload?.let {
                        json.decodeFromJsonElement<MessageDto>(it)
                    } ?: return null

                    MoodMatchEvent.MessageReceived(
                        roomId = dto.randomId,
                        messageId = dto.messageId,
                        fromUSerId = dto.senderId,
                        text = dto.text,
                        sendAt = dto.ts
                    )
                }

                "typing" -> {
                    val dto = env.payload?.let {
                        json.decodeFromJsonElement<TypingDto>(it)
                    } ?: return null

                    MoodMatchEvent.Typing(
                        roomId = dto.roomId,
                        isTyping = dto.isTyping
                    )
                }
                
                "partner_left" -> {
                    val dto = env.payload?.let {  
                        json.decodeFromJsonElement<PartnerLeftDto>(it)
                    } ?: return null

                    MoodMatchEvent.PartnerLeft(
                        roomId = dto.roomId
                    )
                }

                "queue_left" -> {
                    MoodMatchEvent.QueueLeft
                }

                // 🔹 Heartbeat
                "heartbeat_ack" -> {
                    MoodMatchEvent.HeartbeatAck
                }

                "error" -> {
                    val dto = env.payload?.let {
                        json.decodeFromJsonElement<ErrorDto>(it)
                    } ?: return null

                    MoodMatchEvent.Error(
                        code = dto.code,
                        message = dto.message
                    )
                }

                else -> null
            }
        } catch (e: Exception){
            Log.d("ExceptionLog", "parse: exception occurred while parsing MoodMatch event from json payload: $e")
            null
        }
    }

}

