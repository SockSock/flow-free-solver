package com.socksock.flowfreesolver.view

import android.content.Context
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class OverlayView(context: Context) : FrameLayout(context) {
    var listener: (() -> Unit)? = null

    init {
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val solveButton = Button(context).apply {
            text = "Solve"

            setOnClickListener {
                listener?.invoke()
            }
        }

        buttonContainer.addView(solveButton)

        val buttonParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )

        buttonParams.gravity = Gravity.TOP or Gravity.START
        buttonParams.leftMargin = 40
        buttonParams.topMargin = 80

        addView(buttonContainer, buttonParams)
    }
}
