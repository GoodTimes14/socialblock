package it.socialblock.domain.limits

import it.socialblock.domain.model.TrackedApp
import it.socialblock.domain.model.TrackedAppUsage
import javax.inject.Inject

sealed interface LimitStatus {
    data object Available : LimitStatus

    data class Approaching(val remainingMillis: Long) : LimitStatus

    data object Reached : LimitStatus

    data object Bypassed : LimitStatus
}

class LimitEvaluator @Inject constructor() {
    fun evaluate(app: TrackedApp, usedMillis: Long, bypassActive: Boolean): LimitStatus {
        if (bypassActive || !app.timerEnabled) return LimitStatus.Bypassed

        val limitMillis = app.dailyLimitMinutes * TrackedAppUsage.MILLIS_PER_MINUTE
        val remaining = limitMillis - usedMillis
        return when {
            remaining <= 0L -> LimitStatus.Reached
            remaining <= APPROACHING_WINDOW_MILLIS -> LimitStatus.Approaching(remaining)
            else -> LimitStatus.Available
        }
    }

    companion object {
        const val APPROACHING_WINDOW_MILLIS = 5 * TrackedAppUsage.MILLIS_PER_MINUTE
    }
}
