package it.socialblock.usage

import android.app.usage.UsageEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageEventTypeMappingTest {
    @Test
    fun `maps resume and pause boundaries`() {
        assertEquals(
            UsageEventType.FOREGROUND,
            UsageEvents.Event.ACTIVITY_RESUMED.toUsageEventType(),
        )
        assertEquals(
            UsageEventType.BACKGROUND,
            UsageEvents.Event.ACTIVITY_PAUSED.toUsageEventType(),
        )
    }

    @Test
    fun `ignores stopped activity from a package that may still be foreground`() {
        assertNull(UsageEvents.Event.ACTIVITY_STOPPED.toUsageEventType())
    }
}
