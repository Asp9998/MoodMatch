package com.aryanspatel.moodmatch.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aryanspatel.moodmatch.domain.Crypto.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object UserPreference {

    private val Context.dataStore by preferencesDataStore("user_prefs")

    private val AUTH_TOKEN = stringPreferencesKey("auth_token_encrypted")
    private val ONBOARDING_STATUS = booleanPreferencesKey("onboarding_status")

    suspend fun setOnboardingStatusFinished(context: Context){
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_STATUS] = true
        }
    }

    fun getOnboardingStatus(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[ONBOARDING_STATUS] ?: false
        }

    suspend fun setAuthToken(
        context: Context,
        token: String?,
        crypto: CryptoManager
    ) {
        context.dataStore.edit { prefs ->
            if(token == null){
                prefs.remove(AUTH_TOKEN)
            } else {
                val encrypted = crypto.encrypt(token)
                prefs[AUTH_TOKEN] = encrypted
            }
        }
    }

    fun authTokenFlow(
        context: Context,
        crypto: CryptoManager
    ): Flow<String?> =
        context.dataStore.data.map { prefs ->
            val encrypted  = prefs[AUTH_TOKEN] ?: return@map null
            try {
                crypto.decrypt(encrypted)
            } catch (e: Exception){
                null
            }
        }
}