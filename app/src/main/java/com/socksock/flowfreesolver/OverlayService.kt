package com.socksock.flowfreesolver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: FrameLayout

    override fun onCreate() {
        super.onCreate()

        startForeground(
            1,
            createNotification()
        )

        showOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    private fun showOverlay() {

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = FrameLayout(this)

        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val solveButton = Button(this).apply {
            text = "Solve"

            setOnClickListener {
                Log.d("Overlay", "Solve clicked")
            }
        }

        buttonContainer.addView(solveButton)

        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        buttonParams.gravity =
            Gravity.TOP or Gravity.START

        buttonParams.leftMargin = 40
        buttonParams.topMargin = 80

        overlayView.addView(
            buttonContainer,
            buttonParams
        )

        val windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowParams.gravity =
            Gravity.TOP or Gravity.START

        windowManager.addView(
            overlayView,
            windowParams
        )
    }


    private fun createNotification(): Notification {

        val channelId = "overlay_channel"

        val channel = NotificationChannel(
            channelId,
            "Overlay",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Flow Free Solver")
            .setContentText("Overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}