package com.llacskon33.bot1_0.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.llacskon33.bot1_0.R
import com.llacskon33.bot1_0.capture.ScreenCaptureHelper
import com.llacskon33.bot1_0.capture.ScreenCaptureService

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private var receiverRegistered = false

    private val capturePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_START)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            ContextCompat.startForegroundService(this, serviceIntent)
            statusText.text = getString(R.string.capture_starting)
        } else {
            statusText.text = getString(R.string.capture_permission_denied)
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ScreenCaptureService.EXTRA_STATUS)) {
                ScreenCaptureService.STATUS_RUNNING -> statusText.text = getString(R.string.capture_running)
                ScreenCaptureService.STATUS_STOPPED -> statusText.text = getString(R.string.capture_stopped)
                ScreenCaptureService.STATUS_ERROR -> statusText.text = getString(R.string.capture_error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.app_name)
                textSize = 26f
            })
            statusText = TextView(this@MainActivity).apply {
                text = getString(R.string.capture_stopped)
                textSize = 18f
            }
            addView(statusText)
            addView(Button(this@MainActivity).apply {
                text = getString(R.string.start_capture)
                setOnClickListener {
                    capturePermission.launch(ScreenCaptureHelper(this@MainActivity).createCaptureIntent())
                }
            }, matchWidth())
            addView(Button(this@MainActivity).apply {
                text = getString(R.string.stop_capture)
                setOnClickListener {
                    stopService(
                        Intent(this@MainActivity, ScreenCaptureService::class.java)
                            .setAction(ScreenCaptureService.ACTION_STOP)
                    )
                    statusText.text = getString(R.string.capture_stopped)
                }
            }, matchWidth())
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.capture_limitation)
                textSize = 14f
                setPadding(0, 24, 0, 0)
            })
        }

        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ScreenCaptureService.ACTION_STATUS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
