package it.socialblock.platform.format

fun formatDuration(millis: Long): String {
    val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
        hours > 0 -> "${hours}h"
        else -> "$minutes min"
    }
}
