package it.socialblock.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPositionTest {
    @Test
    fun `restores every persisted position`() {
        OverlayPosition.entries.forEach { position ->
            assertEquals(position, OverlayPosition.fromStoredValue(position.name))
        }
    }

    @Test
    fun `uses center for a missing or unknown persisted value`() {
        assertEquals(OverlayPosition.CENTER, OverlayPosition.fromStoredValue(null))
        assertEquals(OverlayPosition.CENTER, OverlayPosition.fromStoredValue("UNKNOWN"))
    }
}
