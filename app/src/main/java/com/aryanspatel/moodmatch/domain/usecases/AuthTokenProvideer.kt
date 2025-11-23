package com.aryanspatel.moodmatch.domain.usecases

import android.content.Context
import com.aryanspatel.moodmatch.domain.Crypto.CryptoManager
import com.aryanspatel.moodmatch.data.datastore.UserPreference
import com.aryanspatel.moodmatch.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface AuthTokenProvider {
    fun currentToken(): String?
    val tokenFlow: StateFlow<String?>
    suspend fun updateToken(token: String?)
}

class AuthTokenProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val cryptoManager: CryptoManager
): AuthTokenProvider{

    private val _token = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _token.asStateFlow()

    init {
        scope.launch {
            UserPreference.authTokenFlow(context, cryptoManager).collect { stored ->
                _token.value = stored
            }
        }
    }

    override fun currentToken(): String? = _token.value

    override suspend fun updateToken(token: String?) {
        _token.value = token
        UserPreference.setAuthToken(context, token, cryptoManager)
    }

}