package it.socialblock.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {
    @Query("SELECT * FROM tracked_apps ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps ORDER BY displayName COLLATE NOCASE")
    suspend fun getAll(): List<TrackedAppEntity>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName")
    suspend fun get(packageName: String): TrackedAppEntity?

    @Upsert
    suspend fun upsert(app: TrackedAppEntity)

    @Query("DELETE FROM tracked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query(
        "UPDATE tracked_apps SET lastApproachingNoticeDate = :localDate " +
            "WHERE packageName = :packageName",
    )
    suspend fun markApproachingNotice(packageName: String, localDate: String)

    @Query(
        "UPDATE tracked_apps SET lastLimitNoticeDate = :localDate " +
            "WHERE packageName = :packageName",
    )
    suspend fun markLimitNotice(packageName: String, localDate: String)
}
