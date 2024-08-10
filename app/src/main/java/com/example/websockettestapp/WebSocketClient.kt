package com.example.websocketdemo

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.util.Log

class SimpleWebSocketListener(private val onConnectionStatusChanged: (Boolean) -> Unit) : WebSocketListener() {
    private var isConnected = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        isConnected = true
        onConnectionStatusChanged(isConnected)
        Log.d("WebSocket", "Connected")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        isConnected = false
        onConnectionStatusChanged(isConnected)
        Log.d("WebSocket", "Closing: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        isConnected = false
        onConnectionStatusChanged(isConnected)
        Log.e("WebSocket", "Error: ${t.message}")
    }

}
