package com.example.websockettestapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.websocketdemo.SimpleWebSocketListener
import com.google.android.gms.location.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import java.util.concurrent.TimeUnit


class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var webSocket: WebSocket
    private var isConnected = false
    private val messageQueue = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the foreground service immediately
        startForegroundService()

        // Initialize the WebSocket connection
        initializeWebSocket()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        val channelId = "LocationServiceChannel"
        val channelName = "Location Service Channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Sending location data...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun initializeWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://ff4e-122-176-232-35.ngrok-free.app/ws/test/")
            .build()
        val listener = SimpleWebSocketListener { connected ->
            isConnected = connected
            sendConnectionStatusBroadcast(connected)
            if (connected) {
                onWebSocketConnected()
                // Start the location updates
                startLocationUpdates()
            }
        }
        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 1000
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    for (location in locationResult.locations) {
                        Log.d(
                            "LocationService",
                            "Location received: ${location.latitude}, ${location.longitude}"
                        )
                        sendLocationToWebSocket(location)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToWebSocket(location: Location) {
        val message = location.toWebSocketJson()
        sendMessage(message)
    }

    private fun sendMessage(message: String) {
        if (isConnected) {
            val isMessageSent = webSocket.send(message)
            Log.d("message Send", message)
            Log.d("message Send", isMessageSent.toString())
            if (!isMessageSent) {
                stopLocationUpdates()
                sendConnectionStatusBroadcast(false)
                stopForeground(true)
            }
        } else {
            stopLocationUpdates()
            sendConnectionStatusBroadcast(false)
            stopForeground(true)
        }
    }

    private fun onWebSocketConnected() {
        while (messageQueue.isNotEmpty()) {
            val message = messageQueue.removeAt(0)
            sendMessage(message)
        }
    }

    private fun stopLocationUpdates() {
        if (this::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun sendConnectionStatusBroadcast(connected: Boolean) {
        val intent = Intent("com.example.websockettestapp.CONNECTION_STATUS")
        intent.putExtra("status", connected)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, LocationService::class.java).also {
            it.setPackage(packageName)
        }
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    fun Location.toWebSocketJson(): String {
        return """{
        "text_data": "[${this.latitude},${this.longitude}]"
    }"""
    }
}


