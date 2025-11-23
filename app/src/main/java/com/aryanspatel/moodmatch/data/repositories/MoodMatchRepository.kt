package com.aryanspatel.moodmatch.data.repositories

import com.aryanspatel.moodmatch.data.remote.RegisterResponseDto
import com.aryanspatel.moodmatch.data.remote.SessionStorage
import com.aryanspatel.moodmatch.data.remote.UserApi
import com.aryanspatel.moodmatch.data.remote.UserProfileDto
import com.aryanspatel.moodmatch.data.remote.UserSession
import com.aryanspatel.moodmatch.domain.usecases.AuthTokenProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodMatchRepository @Inject constructor(
    private val api: UserApi,
    private val sessionStorage: SessionStorage,
    private val authTokenProvider: AuthTokenProvider
){
    
    suspend fun register(
        nickname: String,
        mood: String,
        avatar: String
    ): UserSession{
        val resp: RegisterResponseDto = api.register(nickname, mood, avatar)

        authTokenProvider.updateToken(resp.authToken)

        val session = UserSession(
            userId = resp.userId,
            profile = resp.profile
        )
        sessionStorage.saveSession(session)
        return session
    }

    // use this from app startup.
    suspend fun ensureSessionLoaded(): UserSession? {
        val existing = sessionStorage.getSession()
        if(existing != null) return existing

        // fetch profile using token
        return try{
            val profile = api.getMe()
            val session = UserSession(
                userId = profile.userId,
                profile = profile
            )
            sessionStorage.saveSession(session)
            session
        } catch (e: Exception){
            // if token is invalid/expired, clear token + session
            // authTokenProvider.updateToken(null)
            // sessionStorage.saveSession(null)
            null
        }
    }

    // Force refresh profile from server.
    suspend fun refreshProfile(): UserProfileDto? {

        return try {
            val profile = api.getMe()
            val current = sessionStorage.getSession()

            val newSession = UserSession(
                userId = current?.userId ?: profile.userId,
                profile = profile
            )

            sessionStorage.saveSession(newSession)
            profile
        } catch (e: Exception){
            // handle 401 here if you want to clear token and session
            null
        }
    }

    suspend fun updatePreferences(
        nickname: String? = null,
        mood: String? = null,
        avatar: String? = null
    ): UserProfileDto? {

        return try {
            val updated = api.updatePreferences(nickname, mood, avatar)
            val current = sessionStorage.getSession()

            val newSession = UserSession(
                userId = current?.userId ?: updated.userId,
                profile = updated
            )
            sessionStorage.saveSession(newSession)
            updated
        } catch (e: Exception){
            null
        }
    }

    fun sessionFlow(): Flow<UserSession?> = sessionStorage.sessionFlow()
}