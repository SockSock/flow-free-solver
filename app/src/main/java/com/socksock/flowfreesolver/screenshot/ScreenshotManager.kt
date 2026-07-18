package com.socksock.flowfreesolver.screenshot

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.createBitmap

class ScreenshotManager(
    context: Context,
    resultCode: Int,
    projectionData: Intent,
    private val onScreenshotCaptured: (Bitmap) -> Unit
) {
    private val mediaProjection: MediaProjection
    private val imageReader: ImageReader
    private val virtualDisplay: VirtualDisplay

    private val width: Int
    private val height: Int
    private val densityDpi: Int

    private var latestBitmap: Bitmap? = null

    init {
        val windowManager =
            context.getSystemService(WindowManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds

            width = bounds.width()
            height = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)

            width = metrics.widthPixels
            height = metrics.heightPixels
        }

        densityDpi = context.resources.displayMetrics.densityDpi

        val projectionManager =
            context.getSystemService(MediaProjectionManager::class.java)

        mediaProjection =
            requireNotNull(
                projectionManager.getMediaProjection(
                    resultCode,
                    projectionData
                )
            ) {
                "Failed to create MediaProjection."
            }

        imageReader = ImageReader.newInstance(
            width,
            height,
            android.graphics.PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay =
            requireNotNull(
                mediaProjection.createVirtualDisplay(
                    "FlowFreeScreenshot",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    null
                )
            ) {
                "Failed to create VirtualDisplay."
            }
    }

    fun capture() {
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
                ?: return@setOnImageAvailableListener

            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = createBitmap(width + rowPadding / pixelStride, height)

                bitmap.copyPixelsFromBuffer(buffer)

                latestBitmap?.recycle()

                latestBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    width,
                    height
                )

                bitmap.recycle()

                onScreenshotCaptured(latestBitmap!!)
            } finally {
                image.close()
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun close() {
        imageReader.close()
        virtualDisplay.release()
        mediaProjection.stop()
        latestBitmap?.recycle()
        latestBitmap = null
    }
}