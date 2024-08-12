package com.example.websockettestapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.websocketdemo.SimpleWebSocketListener
import com.google.android.gms.location.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class MainActivity : AppCompatActivity() {
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket
    private var isConnected = false
    private lateinit var button: Button
    private lateinit var disbutton: Button
    private lateinit var editText: EditText
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private var recivetext: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.connect)
        disbutton = findViewById(R.id.disconnect)
        editText = findViewById(R.id.textbox)


        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.toString().isNotEmpty()) {
                    recivetext = p0.toString()
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        requestLocationPermissions()

        disbutton.setOnClickListener {
//            if (isConnected) {
//                webSocket.close(1000, "App is disconnecting")
//                isConnected = false
//                stopLocationService()
//                Toast.makeText(this, "Disconnected Successfully", Toast.LENGTH_LONG).show()
//            } else {
//                Toast.makeText(this, "Please connect WebSocket first", Toast.LENGTH_LONG).show()
//            }
            stopLocationService()
            button.isEnabled= true
            disbutton.isEnabled = false
        }






        button.setOnClickListener {
            val intent = Intent(this, LocationService::class.java)
            ContextCompat.startForegroundService(this, intent)
            button.isEnabled= false
            disbutton.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isConnected) {
            webSocket.close(1000, "App is closing")
        }

    }

    private fun initializeWebSocket() {
        client = OkHttpClient()  // Re-create OkHttpClient each time to reset
        val request = Request.Builder()
            .url("wss://demo.piesocket.com/v3/channel_123?api_key=VCXCEuvhGcBDP7XhiJJUDvR1e1D3eiVjgZ9VRiaV&notify_self")
            .build()
        val listener = SimpleWebSocketListener { connected ->
            isConnected = connected
            runOnUiThread {
                if (connected) {
                    Toast.makeText(this, "Connected Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }


    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }


}
