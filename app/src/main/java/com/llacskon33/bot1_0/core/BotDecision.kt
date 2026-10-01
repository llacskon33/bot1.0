package com.llacskon33.bot1_0.core

sealed class BotDecision {
    data object MoveAwayFromDanger : BotDecision()
    data class AimAndAttack(val x: Float, val y: Float) : BotDecision()
    data object FollowAlly : BotDecision()
    data object SearchAndPatrol : BotDecision()
}
