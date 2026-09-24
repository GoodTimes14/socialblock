package it.socialblock.data.local

import androidx.room.Entity

@Entity(
    tableName = "daily_usage",
    primaryKeys = ["packageName", "localDate"],
)
data class DailyUsageEntity(val packageName: String, val localDate: String, val usedMillis: Long, val updatedAtMillis: Long)
