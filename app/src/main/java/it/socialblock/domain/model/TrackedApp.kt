package it.socialblock.domain.model

data class TrackedApp(
    val packageName: String,
    val displayName: String,
    val dailyLimitMinutes: Int,
    val timerEnabled: Boolean,
    val overlayEnabled: Boolean,
    val lastApproachingNoticeDate: String?,
    val lastLimitNoticeDate: String?,
)

data class TrackedAppUsage(val app: TrackedApp, val usedMillis: Long) {
    val limitMillis: Long = app.dailyLimitMinutes * MILLIS_PER_MINUTE
    val progress: Float =
        if (limitMillis == 0L) 0f else (usedMillis.toFloat() / limitMillis).coerceIn(0f, 1f)

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
