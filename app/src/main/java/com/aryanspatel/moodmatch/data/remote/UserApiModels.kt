package com.aryanspatel.moodmatch.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val userId: String,
    val nickname: String,
    val mood: String,
    val avatar: String,
    val status: String,
    val createdAt: Long,
    val lastSeenAt: Long
)

@Serializable
data class RegisterRequestDto(
    val nickname: String,
    val mood: String,
    val avatar: String
)

@Serializable
data class RegisterResponseDto(
    val userId: String,
    val authToken: String,
    val profile: UserProfileDto
)

@Serializable
data class UpdatePreferencesRequestDto(
    val nickname: String? = null,
    val mood: String? = null,
    val avatar: String? = null
)

@Serializable
data class UserSession(
    val userId: String,
    val profile: UserProfileDto
)

interface SessionStorage {
    suspend fun saveSession(session: UserSession)
    suspend fun getSession(): UserSession?
    fun sessionFlow(): Flow<UserSession?>
}