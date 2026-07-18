package com.socksock.flowfreesolver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.socksock.flowfreesolver.model.ImageParser
import com.socksock.flowfreesolver.screenshot.ScreenshotManager
import com.socksock.flowfreesolver.view.OverlayView

class OverlayService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_PROJECTION_DATA = "projection_data"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: OverlayView
    private lateinit var screenshotManager: ScreenshotManager

    private val parser = ImageParser()

    override fun onCreate() {
        super.onCreate()

        startForeground(
            1,
            createNotification()
        )

        showOverlay()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        stardId: Int
    ): Int {
        val resultCode =
            intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1

        val projectionData =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(
                    EXTRA_PROJECTION_DATA,
                    Intent::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_PROJECTION_DATA)
            }

        if (resultCode != -1 && projectionData != null) {
            screenshotManager = ScreenshotManager(
                context = this,
                resultCode = resultCode,
                projectionData = projectionData
            ) { bitmap ->
                parser.parseImage(bitmap)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }

        if (::screenshotManager.isInitialized) {
            screenshotManager.close()
        }
    }

    private fun showOverlay() {
        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = OverlayView(this)

        overlayView.listener = {
            if (::screenshotManager.isInitialized) {
                screenshotManager.capture()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity =
            Gravity.TOP or Gravity.START

        windowManager.addView(
            overlayView,
            params
        )
    }


    private fun createNotification(): Notification {
        val channelId = "overlay_channel"

        val channel = NotificationChannel(
            channelId,
            "Overlay",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Flow Free Solver")
            .setContentText("Overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}