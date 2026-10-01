package com.llacskon33.bot1_0.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.result.ActivityResultLauncher

/**
 * Helper to request screen-capture consent from the user.
 */
class ScreenCaptureHelper(private val activity: Activity) {
    fun createCaptureIntent(): Intent {
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mgr.createScreenCaptureIntent()
    }
}
