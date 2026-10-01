package com.llacskon33.bot1_0.core

import android.graphics.Bitmap

/**
 * Minimal vision abstraction for Android migration.
 * In the first version, this can be backed by MediaProjection/screen capture.
 */
class VisionEngine {
    fun analyzeFrame(frame: Bitmap): GameState {
        // Placeholder analysis until the full computer vision pipeline is implemented.
        return GameState(
            enemyVisible = false,
            allyVisible = false,
            lowHealth = false,
            inFog = false,
            targetX = null,
            targetY = null
        )
    }
}
