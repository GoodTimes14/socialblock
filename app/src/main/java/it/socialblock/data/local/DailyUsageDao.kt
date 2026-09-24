package it.socialblock.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DailyUsageDao {
    @Upsert
    suspend fun upsertAll(rows: List<DailyUsageEntity>)

    @Query("SELECT * FROM daily_usage WHERE localDate = :localDate")
    suspend fun getForDate(localDate: String): List<DailyUsageEntity>

    @Query("DELETE FROM daily_usage WHERE localDate < :oldestDateToKeep")
    suspend fun deleteOlderThan(oldestDateToKeep: String)
}
