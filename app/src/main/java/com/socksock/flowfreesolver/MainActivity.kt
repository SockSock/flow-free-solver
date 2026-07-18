package com.socksock.flowfreesolver

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.socksock.flowfreesolver.service.OverlayService

class MainActivity : ComponentActivity() {
    private var permissionFlowStarted = false

    private val projectionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            permissionFlowStarted = false

            if (result.resultCode != RESULT_OK || result.data == null) {
                finish()
                return@registerForActivityResult
            }

            val serviceIntent = Intent(
                this,
                OverlayService::class.java
            ).apply {
                putExtra(
                    OverlayService.EXTRA_RESULT_CODE,
                    result.resultCode
                )

                putExtra(
                    OverlayService.EXTRA_PROJECTION_DATA,
                    result.data
                )
            }

            ContextCompat.startForegroundService(
                this,
                serviceIntent
            )

            finish()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onResume() {
        super.onResume()

        if (!permissionFlowStarted) {
            checkPermissions()
        }
    }


    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            permissionFlowStarted = true

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
            )

            return
        }


        requestScreenCapture()
    }


    private fun requestScreenCapture() {
        permissionFlowStarted = true

        val manager =
            getSystemService(MediaProjectionManager::class.java)

        projectionLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }
}