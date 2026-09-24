package it.socialblock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedAppEntity::class, DailyUsageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SocialBlockDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao

    abstract fun dailyUsageDao(): DailyUsageDao
}
