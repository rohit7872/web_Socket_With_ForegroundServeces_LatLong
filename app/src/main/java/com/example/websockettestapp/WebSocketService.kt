//package com.example.websockettestapp
//
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import android.util.Log
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.WebSocket
//
//class WebSocketService : Service() {
//    private lateinit var client: OkHttpClient
//    private lateinit var webSocket: WebSocket
//    private var isConnected = false
//
//    override fun onCreate() {
//        super.onCreate()
//        startForegroundService()
//        initializeWebSocket()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (isConnected) {
//            webSocket.close(1000, "Service is stopping")
//        }
//    }
//
//    private fun initializeWebSocket() {
//        client = OkHttpClient()
//        val request = Request.Builder()
//            .url("wss://cec0-2409-40c2-17-974a-c8f2-973-325e-e69.ngrok-free.app/ws/test/")
//            .build()
//        // Pass `this` (the service context) to the listener
//        val listener = SimpleWebSocketListener(this) { connected ->
//            isConnected = connected
//            if (connected) {
//                Log.d("WebSocketService", "Connected to WebSocket")
//            } else {
//                Log.d("WebSocketService", "Failed to connect to WebSocket")
//            }
//        }
//
//        webSocket = client.newWebSocket(request, listener)
//        client.dispatcher.executorService.shutdown()
//    }
//
//    private fun startForegroundService() {
//        // Start foreground service code here
//    }
//}
