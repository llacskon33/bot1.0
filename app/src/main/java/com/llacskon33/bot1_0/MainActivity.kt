package com.llacskon33.bot1_0.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.llacskon33.bot1_0.core.BrawlStarsBot

class MainActivity : AppCompatActivity() {
    private val bot = BrawlStarsBot()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(TextView(this@MainActivity).apply {
                text = "Bot 1.0"
                textSize = 26f
            })
            addView(TextView(this@MainActivity).apply {
                text = "Android base + capture service ready"
                textSize = 18f
            })
        }

        setContentView(root)
    }
}
