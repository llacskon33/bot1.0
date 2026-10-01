package com.llacskon33.bot1_0.input

import android.view.MotionEvent
import android.view.View

/**
 * Placeholder input controller for Android. Real implementation may use AccessibilityService,
 * overlay controls, or in-app gesture handling depending on the final architecture.
 */
class InputController(private val view: View) {
    fun tap(x: Float, y: Float) {
        // Intentionally minimal placeholder.
        view.dispatchTouchEvent(
            MotionEvent.obtain(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
            )
        )
        view.dispatchTouchEvent(
            MotionEvent.obtain(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                MotionEvent.ACTION_UP,
                x,
                y,
                0
            )
        )
    }
}
