package com.llacskon33.bot1_0.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.llacskon33.bot1_0.core.BotDecision
import com.llacskon33.bot1_0.core.BrawlStarsBot

class MainActivity : AppCompatActivity() {
    private val bot = BrawlStarsBot()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this).apply {
            text = "Bot 1.0\nAndroid migration in progress"
            textSize = 20f
            setPadding(48, 48, 48, 48)
        }
        setContentView(tv)
    }

    fun debugDecision(): String {
        val decision = BotDecision.SearchAndPatrol
        return "Decision: $decision"
    }
}
