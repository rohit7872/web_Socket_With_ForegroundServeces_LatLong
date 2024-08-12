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
import com.example.websocketdemo.SimpleWebSocketListener
import com.google.android.gms.location.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var webSocket: WebSocket
    private var isConnected = false
    private val messageQueue = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()

        // Initialize the WebSocket connection
        initializeWebSocket()
    }

//    override fun onLocationResult(locationResult: LocationResult) {
//        if (locationResult.locations.isNotEmpty()) {
//            for (location in locationResult.locations) {
//                Log.d(
//                    "LocationService",
//                    "Location received: ${location.latitude}, ${location.longitude}"
//                )
//                sendLocationToWebSocket(location)
//            }
//        }
//    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initializeWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://demo.piesocket.com/v3/channel_123?api_key=VCXCEuvhGcBDP7XhiJJUDvR1e1D3eiVjgZ9VRiaV&notify_self")
            .build()
        val listener = SimpleWebSocketListener { connected ->
            isConnected = connected
            if (connected) {
                onWebSocketConnected()
                // Initialize the FusedLocationProviderClient
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                // Start the location updates
                startLocationUpdates()
                // Start the foreground service with a notification
                startForegroundService()
            }
        }
        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    //    private fun startLocationUpdates() {
//        val locationRequest = LocationRequest.create().apply {
//            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
//            interval = 1000 // Request location updates every 1 second
//            fastestInterval = 1000 // Allow the fastest location updates to be every 1 second
//        }
//
////        locationCallback = object : LocationCallback() {
////            override fun onLocationResult(locationResult: LocationResult) {
////                if (locationResult.locations.isNotEmpty()) {
////                    for (location in locationResult.locations) {
////                        Log.d("LocationService", "Location send: ${location.latitude}, ${location.longitude}")
////                        sendLocationToWebSocket(location)
////                    }
////                }
////            }
////        }
//
////        locationCallback = object : LocationCallback() {
////            override fun onLocationResult(locationResult: LocationResult) {
////                for (location in locationResult.locations) {
////                    sendLocationToWebSocket(location)
////                }
////            }
////        }
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Log.d("LocationService", "Location permissions not granted")
//            return
//        }
//
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )
//    }
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000 // Request location updates every 1 second
            fastestInterval = 1000 // Allow the fastest location updates to be every 1 second
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    for (location in locationResult.locations) {
                        Log.d("LocationService", "Location received: ${location.latitude}, ${location.longitude}")
                        sendLocationToWebSocket(location)
                    }
                } else {
                    Log.d("LocationService", "No location result received")
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationService", "Location permissions not granted")
            return
        }

        Log.d("LocationService", "Requesting location updates")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    private fun sendLocationToWebSocket(location: Location) {
        val message = "Lat: ${location.latitude}, Long: ${location.longitude}"
        sendMessage(message)
    }

    private fun sendMessage(message: String) {
        if (isConnected) {
            val isMessageSent = webSocket.send(message)
            if (isMessageSent) {
                Log.d("WebSocket", "Message successfully sent: $message")
            } else {
                Log.d("WebSocket", "Failed to send message: $message")
            }
        } else {
            Log.d("WebSocket", "WebSocket not connected, queuing message: $message")
            messageQueue.add(message)
        }
    }

    private fun onWebSocketConnected() {
        isConnected = true
        while (messageQueue.isNotEmpty()) {
            val message = messageQueue.removeAt(0)
            sendMessage(message)
        }
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

    private fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
