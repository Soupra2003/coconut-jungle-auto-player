package com.example.coconutautoplayer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.provider.Settings
import android.text.TextUtils

class GameAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: GameAccessibilityService? = null
        @Volatile var lastGestureAccepted: Boolean = false

        fun isEnabled(context: android.content.Context): Boolean {
            val expected = "${context.packageName}/${GameAccessibilityService::class.java.name}"
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            return splitter.any { it.equals(expected, ignoreCase = true) }
        }
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { releaseGesture() }
    override fun onDestroy() {
        releaseGesture()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private var lastX = -1f
    private var lastY = -1f
    private var stroke: GestureDescription.StrokeDescription? = null

    @Synchronized
    fun moveFinger(x: Float, y: Float, screenWidth: Int, screenHeight: Int) {
        val nx = x.coerceIn(2f, screenWidth - 2f)
        val ny = y.coerceIn(2f, screenHeight - 2f)
        val path = Path()
        if (lastX < 0f || stroke == null) {
            path.moveTo(nx, ny)
            path.lineTo(nx + 0.5f, ny + 0.5f)
            val initial = GestureDescription.StrokeDescription(path, 0, 350, true)
            val gesture = GestureDescription.Builder().addStroke(initial).build()
            lastX = nx; lastY = ny
            stroke = initial
            lastGestureAccepted = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    synchronized(this@GameAccessibilityService) { stroke = null; lastX = -1f; lastY = -1f }
                }
            }, null)
        } else {
            val continuationPath = Path()
            continuationPath.moveTo(lastX, lastY)
            continuationPath.lineTo(nx, ny)
            val continued = try {
                stroke!!.continueStroke(continuationPath, 0, 350, true)
            } catch (_: Exception) {
                stroke = null
                lastX = -1f; lastY = -1f
                return
            }
            val gesture = GestureDescription.Builder().addStroke(continued).build()
            lastX = nx; lastY = ny; stroke = continued
            lastGestureAccepted = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    synchronized(this@GameAccessibilityService) { stroke = null; lastX = -1f; lastY = -1f }
                }
            }, null)
        }
    }

    @Synchronized
    fun releaseGesture() {
        stroke = null
        lastX = -1f
        lastY = -1f
    }
}
