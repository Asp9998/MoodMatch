package com.aryanspatel.moodmatch.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aryanspatel.moodmatch.data.remote.SessionStorage
import com.aryanspatel.moodmatch.data.remote.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// Separate DataStore for session
private val Context.sessionDataStore by preferencesDataStore("session_prefs")
private val USER_SESSION_JSON = stringPreferencesKey("user_session_json")

@Singleton
class SessionStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val scope: CoroutineScope
): SessionStorage {
    private val _sessionFlow = MutableStateFlow<UserSession?>(null)
    override fun sessionFlow(): Flow<UserSession?> = _sessionFlow.asStateFlow()

    init {
        // Keep in-memory cache in sync with DataStore
        scope.launch {
            context.sessionDataStore.data
                .map { prefs -> prefs[USER_SESSION_JSON] }
                .collect { encoded ->
                    _sessionFlow.value = encoded?.let {
                        try {
                            json.decodeFromString<UserSession>(it)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
        }
    }

    override suspend fun saveSession(session: UserSession) {
        val encoded = json.encodeToString(session)
        context.sessionDataStore.edit { prefs ->
            prefs[USER_SESSION_JSON] = encoded
        }
    }

    override suspend fun getSession(): UserSession? = _sessionFlow.value

}