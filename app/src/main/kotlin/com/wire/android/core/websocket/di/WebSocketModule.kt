package com.wire.android.core.websocket.di

import com.wire.android.core.network.auth.accesstoken.AccessTokenInterceptor
import com.wire.android.core.websocket.data.WebSocketProvider
import com.wire.android.core.websocket.WebSocketConfig
import com.wire.android.core.websocket.data.EventRepository
import com.wire.android.core.websocket.data.WireWebSocketListener
import com.wire.android.core.websocket.usecase.CloseWebSocketUseCase
import com.wire.android.core.websocket.usecase.ListenToWebSocketUseCase
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit


private fun buildSocket(socketOkHttpClient: OkHttpClient, config: WebSocketConfig, webSocketListener: WireWebSocketListener) : WebSocket {
    val webSocket = socketOkHttpClient.newWebSocket(
        Request.Builder().url(config.socketUrl).build(),
        webSocketListener
    )
    socketOkHttpClient.dispatcher.executorService.shutdown()
    return webSocket
}


private fun socketHttpClient(accessTokenInterceptor: AccessTokenInterceptor): OkHttpClient =
    OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .addInterceptor(accessTokenInterceptor)
        .hostnameVerifier { _, _ -> true }
        .build()


val webSocketModule = module {
    factory { WebSocketProvider(get(), get()) }
    factory { EventRepository(get()) }
    factory { CloseWebSocketUseCase(get()) }
    factory { ListenToWebSocketUseCase(get()) }
    single { WebSocketConfig() }
    val webSocketClient = "WEB_SOCKET_CLIENT"
    single(named(webSocketClient)) {
        buildSocket(socketHttpClient(get()), get(), get())
    }
    single { buildSocket(get(), get(), get()) }
    single { WireWebSocketListener() }
}