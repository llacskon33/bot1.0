package com.llacskon33.bot1_0.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import com.llacskon33.bot1_0.core.BrawlStarsBot

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private val bot = BrawlStarsBot()
    private var lastProcessedAt = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseCapture(stopProjection = false)
            sendStatus(STATUS_STOPPED)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseCapture(stopProjection = true)
                sendStatus(STATUS_STOPPED)
                stopSelf()
            }

            ACTION_START -> startCapture(intent)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseCapture(stopProjection = true)
        super.onDestroy()
    }

    private fun startCapture(intent: Intent) {
        if (projection != null) return

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent.parcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            sendStatus(STATUS_ERROR)
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("Preparing screen capture"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Preparing screen capture"))
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = manager.getMediaProjection(resultCode, resultData)
            projection = mediaProjection
            val (width, height, density) = screenSize()
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            val thread = HandlerThread("BotScreenCapture").apply { start() }

            captureThread = thread
            mediaProjection.registerCallback(projectionCallback, Handler(thread.looper))
            reader.setOnImageAvailableListener({ source -> processLatestFrame(source) }, Handler(thread.looper))
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "bot1-screen-capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                Handler(thread.looper)
            )
            updateNotification("Screen capture active")
            sendStatus(STATUS_RUNNING)
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to start screen capture", exception)
            releaseCapture(stopProjection = true)
            sendStatus(STATUS_ERROR)
            stopSelf()
        }
    }

    private fun processLatestFrame(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (exception: IllegalStateException) {
            Log.w(TAG, "Unable to acquire screen frame", exception)
            null
        } ?: return

        try {
            val now = System.currentTimeMillis()
            if (now - lastProcessedAt < FRAME_INTERVAL_MS) return
            lastProcessedAt = now

            val bitmap = image.toBitmap()
            try {
                val decision = bot.processFrame(bitmap)
                Log.d(TAG, "Frame analyzed: ${decision.javaClass.simpleName}")
            } finally {
                bitmap.recycle()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to analyze screen frame", exception)
        } finally {
            image.close()
        }
    }

    private fun screenSize(): Triple<Int, Int, Int> {
        val density = resources.displayMetrics.densityDpi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = (getSystemService(WindowManager::class.java)).currentWindowMetrics.bounds
            return Triple(bounds.width(), bounds.height(), density)
        }

        @Suppress("DEPRECATION")
        val metrics = DisplayMetrics().also {
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(it)
        }
        return Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
    }

    private fun releaseCapture(stopProjection: Boolean) {
        val currentProjection = projection
        projection = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        captureThread?.quitSafely()
        captureThread = null
        if (currentProjection != null) {
            currentProjection.unregisterCallback(projectionCallback)
            if (stopProjection) currentProjection.stop()
        }
    }

    private fun sendStatus(status: String) {
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).putExtra(EXTRA_STATUS, status))
    }

    private fun buildNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("bot1.0")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            buildNotification(message)
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes[0]
        val pixelStride = plane.pixelStride
        val rowPadding = plane.rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride
        val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        paddedBitmap.copyPixelsFromBuffer(plane.buffer)
        val bitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
        paddedBitmap.recycle()
        return bitmap
    }

    private inline fun <reified T : android.os.Parcelable> Intent.parcelableExtra(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
        }
    }

    companion object {
        const val ACTION_START = "com.llacskon33.bot1_0.capture.START"
        const val ACTION_STOP = "com.llacskon33.bot1_0.capture.STOP"
        const val ACTION_STATUS = "com.llacskon33.bot1_0.capture.STATUS"
        const val EXTRA_RESULT_CODE = "capture_result_code"
        const val EXTRA_RESULT_DATA = "capture_result_data"
        const val EXTRA_STATUS = "capture_status"
        const val STATUS_RUNNING = "running"
        const val STATUS_STOPPED = "stopped"
        const val STATUS_ERROR = "error"

        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "bot1_capture"
        private const val NOTIFICATION_ID = 1001
        private const val FRAME_INTERVAL_MS = 200L
    }
}
