package com.socksock.flowfreesolver

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:&packageName".toUri()
            )
            startActivity(intent)
        } else {
            startOverlay()
        }
    }

    private fun startOverlay() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, OverlayService::class.java)
        )
    }
}