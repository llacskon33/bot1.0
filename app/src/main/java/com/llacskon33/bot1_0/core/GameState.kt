package com.llacskon33.bot1_0.core

data class GameState(
    val enemyVisible: Boolean,
    val allyVisible: Boolean,
    val lowHealth: Boolean,
    val inFog: Boolean,
    val targetX: Float?,
    val targetY: Float?
)
