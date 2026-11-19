package com.example.coconutautoplayer

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    private val REQUEST_CAPTURE = 701

    @Deprecated("Using the platform activity-result callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val intent = Intent(this, CaptureService::class.java).apply {
                    action = CaptureService.ACTION_START
                    putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(CaptureService.EXTRA_RESULT_DATA, data)
                }
                startForegroundService(intent)
                status.text = "Screen capture started. Open Coconut Jungle Rush and keep this service running."
            } else {
                status.text = "Screen capture permission was not granted."
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val heading = TextView(this).apply {
            text = "🥥 Coconut Auto Player"
            textSize = 25f
            gravity = Gravity.CENTER
        }
        val intro = TextView(this).apply {
            text = "Prototype for Coconut Jungle Rush\\n\\n1. Enable the gesture service.\\n2. Grant screen-capture permission.\\n3. Switch to the game.\\n4. Start Auto Play from this app."
            textSize = 16f
            setPadding(0, pad, 0, pad)
        }
        val accessibility = Button(this).apply {
            text = "1. Enable gesture accessibility"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val start = Button(this).apply {
            text = "2. Grant capture & start"
            setOnClickListener {
                if (!GameAccessibilityService.isEnabled(this@MainActivity)) {
                    status.text = "Enable Coconut Auto Player in Android Accessibility settings first."
                } else {
                    val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
                }
            }
        }
        val stop = Button(this).apply {
            text = "Stop automation"
            setOnClickListener {
                startService(Intent(this@MainActivity, CaptureService::class.java).setAction(CaptureService.ACTION_STOP))
                status.text = "Stop requested."
            }
        }
        status = TextView(this).apply {
            text = "Status: idle"
            textSize = 14f
            setPadding(0, pad, 0, 0)
        }
        layout.addView(heading)
        layout.addView(intro)
        layout.addView(accessibility)
        layout.addView(start)
        layout.addView(stop)
        layout.addView(status)
        setContentView(layout)
    }
}
