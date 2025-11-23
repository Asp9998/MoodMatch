package com.aryanspatel.moodmatch.di

import android.content.Context
import com.aryanspatel.moodmatch.data.datastore.SessionStorageImpl
import com.aryanspatel.moodmatch.domain.Crypto.CryptoManager
import com.aryanspatel.moodmatch.data.remote.ApiConfig
import com.aryanspatel.moodmatch.data.remote.SessionStorage
import com.aryanspatel.moodmatch.data.soket.client.MoodMatchWebSocketClient
import com.aryanspatel.moodmatch.data.soket.client.MoodMatchWebSocketClientImpl
import com.aryanspatel.moodmatch.data.soket.client.WebSocketEventParser
import com.aryanspatel.moodmatch.domain.usecases.AuthTokenProvider
import com.aryanspatel.moodmatch.domain.usecases.AuthTokenProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton @ApplicationScope
    fun provideApplicationScope(): CoroutineScope{
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides @Singleton
    fun provideAuthTokenProvider(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        cryptoManager: CryptoManager
    ): AuthTokenProvider = AuthTokenProviderImpl(context, scope, cryptoManager)

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true     // tolerate extra fields from backend
        isLenient = true             // be flexible with JSON
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides @Singleton
    fun provideHttpClient(
        json: Json,
        authTokenProvider: AuthTokenProvider
    ): HttpClient{
        return HttpClient(OkHttp){
            install(ContentNegotiation){
                json(json)
            }

            defaultRequest {
                url{
                    host = ApiConfig.httpHost
                    port = ApiConfig.httpPort
                    protocol = ApiConfig.httpProtocol
                }

                authTokenProvider.currentToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            install(Logging){
                level = LogLevel.BODY
            }

            install(WebSockets){
                // optional: app-level pings will happen in our socket client,
                // but this is also useful for low-level health.
                pingIntervalMillis = 30_000
            }

            install(HttpTimeout){
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }

            engine {
                // OkHttp-specific tuning
                config {
                    retryOnConnectionFailure(true)
                }
            }
        }
    }

    @Provides @Singleton
    fun provideWebSocketEventParser(json: Json): WebSocketEventParser =
        WebSocketEventParser(json)

    @Provides @Singleton
    fun provideMoodMatchWebSocketClient(
        client: HttpClient,
        parser: WebSocketEventParser,
        json: Json,
        @ApplicationScope scope: CoroutineScope,
        authTokenProvider: AuthTokenProvider
    ): MoodMatchWebSocketClient = MoodMatchWebSocketClientImpl(
        client = client,
        parser = parser,
        json = json,
        scope = scope,
        authTokenProvider = authTokenProvider
    )

    @Provides @Singleton
    fun provideSessionStorage(
        @ApplicationContext context: Context,
        json: Json,
        @ApplicationScope scope: CoroutineScope
    ): SessionStorage = SessionStorageImpl(context, json, scope)
}