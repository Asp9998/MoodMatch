package com.aryanspatel.moodmatch.data.remote

import io.ktor.http.URLProtocol

object ApiConfig {

    private const val PROD_HOST = "moodmatch-backend-dr9e.onrender.com"
    val httpHost: String
//        get() =  "10.0.2.2"
        get() = PROD_HOST

    val httpPort: Int
//        get() = 8080
        get() = 443

    val httpProtocol: URLProtocol
//        get() = URLProtocol.HTTP
        get() = URLProtocol.HTTPS

    val wsUrl: String
//        get() = "ws://10.0.2.2:8080/ws"
        get() = "wss://$PROD_HOST/ws"
}