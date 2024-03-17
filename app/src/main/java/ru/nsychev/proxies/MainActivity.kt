package ru.nsychev.proxies

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var receiver: BroadcastReceiver
    private var state: ProxyState = ProxyState.INACTIVE
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val started = intent.getBooleanExtra("started", false)
                val reason = intent.getStringExtra("reason")
                actionCompleted(started, context, reason)
            }
        }

        button = findViewById(R.id.button)
        button.setOnClickListener {
            when (state) {
                ProxyState.INACTIVE -> startAfterNotification(applicationContext)
                ProxyState.ACTIVE -> stop()
                else -> {}
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(INTENT_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun startAfterNotification(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            start()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun start() {
        state = ProxyState.LOADING
        button.text = getString(R.string.proxy_button_loading)
        button.isEnabled = false
        val serviceIntent = Intent(this, ProxyService::class.java)
        startService(serviceIntent)
    }

    private fun stop() {
        state = ProxyState.LOADING
        button.text = getString(R.string.proxy_button_loading)
        button.isEnabled = false
        val serviceIntent = Intent(this, ProxyService::class.java)
        stopService(serviceIntent)
    }

    private fun actionCompleted(started: Boolean, context: Context?, reason: String?) {
        val newState = if (started) ProxyState.ACTIVE else ProxyState.INACTIVE
        if (newState == state) {
            return
        }
        state = newState
        button.text =
            getString(if (started) R.string.proxy_button_active else R.string.proxy_button_inactive)
        button.isEnabled = true

        if (reason == null) {
            val toastMessage = if (started) R.string.proxy_started_message else R.string.proxy_stopped_message
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
        }
    }
}