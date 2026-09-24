package it.socialblock.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SocialBlockDatabaseTest {
    private lateinit var database: SocialBlockDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SocialBlockDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun trackedAppRoundTripsThroughRoom() = runBlocking {
        val entity =
            TrackedAppEntity(
                packageName = "com.example.social",
                displayName = "Social",
                dailyLimitMinutes = 30,
                timerEnabled = true,
                overlayEnabled = true,
                createdAtMillis = 1L,
            )
        database.trackedAppDao().upsert(entity)

        assertEquals(entity, database.trackedAppDao().observeAll().first().single())
    }
}
