package it.socialblock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_apps")
data class TrackedAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val dailyLimitMinutes: Int,
    val timerEnabled: Boolean,
    val overlayEnabled: Boolean,
    val lastApproachingNoticeDate: String? = null,
    val lastLimitNoticeDate: String? = null,
    val createdAtMillis: Long,
)
