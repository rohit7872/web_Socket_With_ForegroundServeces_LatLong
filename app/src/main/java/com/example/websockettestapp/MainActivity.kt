package com.example.websockettestapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity()  {

    private lateinit var button: Button
    private lateinit var disbutton: Button
    private lateinit var editText: EditText
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private var recivetext: String = ""
    private var statusConnected:MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var connectionStatusReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.connect)
        disbutton = findViewById(R.id.disconnect)
        editText = findViewById(R.id.textbox)

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recivetext = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        requestLocationPermissions()

        button.setOnClickListener {
            val intent = Intent(this, LocationService::class.java)
            ContextCompat.startForegroundService(this, intent)
            button.isEnabled = false
            disbutton.isEnabled = true
        }

        disbutton.setOnClickListener {
            stopLocationService()
            button.isEnabled = true
            disbutton.isEnabled = false
        }

        // Initialize and register BroadcastReceiver
        connectionStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getBooleanExtra("status", false) ?: false
                runOnUiThread {
                    if (status) {
                        button.isEnabled = false
                        disbutton.isEnabled = true
                        Toast.makeText(this@MainActivity, "WebSocket Connected", Toast.LENGTH_SHORT).show()
                    } else {
                        button.isEnabled = true
                        disbutton.isEnabled = false
                        Toast.makeText(this@MainActivity, "WebSocket Disconnected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            connectionStatusReceiver,
            IntentFilter("com.example.websockettestapp.CONNECTION_STATUS")
        )
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
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        } else {
            Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStatusReceiver)
    }


}
