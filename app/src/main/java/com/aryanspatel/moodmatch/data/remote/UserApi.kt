package com.aryanspatel.moodmatch.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import javax.inject.Inject
import javax.inject.Singleton
import io.ktor.http.ContentType.Application.Json
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders

@Singleton
class UserApi @Inject constructor (
    private val client: HttpClient
){
    suspend fun register(
        nickname: String,
        mood: String,
        avatar: String
    ): RegisterResponseDto {
        return client.post("/api/users/register") {
            headers {
                append(HttpHeaders.ContentType, Json.toString())
            }
            setBody(RegisterRequestDto(
                nickname = nickname,
                mood = mood,
                avatar = avatar
            ))
        }.body()
    }

    suspend fun getMe(): UserProfileDto {
        return client.get("/api/users/me"){
            headers {
                append(HttpHeaders.ContentType, Json.toString())
            }
        }.body()
    }

    suspend fun updatePreferences(
        nickname: String? = null,
        mood: String? = null,
        avatar: String? = null
    ): UserProfileDto{
        return client.put("/api/users/me"){
            headers {
                append(HttpHeaders.ContentType, Json.toString())
            }
            setBody(
                UpdatePreferencesRequestDto(
                    nickname = nickname,
                    mood = mood,
                    avatar = avatar
                )
            )
        }.body()
    }


}