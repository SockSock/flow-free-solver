package com.socksock.flowfreesolver

import android.app.Service
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    override fun onCreate() {
        super.onCreate();
    }
}