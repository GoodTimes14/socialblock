package it.socialblock.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsTest {
    @Test
    fun `default overlay opacity matches the existing semitransparent level`() {
        assertEquals(0.78f, AppSettings().overlayOpacity, FLOAT_TOLERANCE)
    }

    @Test
    fun `overlay opacity is clamped to the touch safe range`() {
        assertEquals(
            0.2f,
            AppSettings(overlayOpacityPercent = 0).overlayOpacity,
            FLOAT_TOLERANCE,
        )
        assertEquals(
            0.8f,
            AppSettings(overlayOpacityPercent = 100).overlayOpacity,
            FLOAT_TOLERANCE,
        )
    }

    @Test
    fun `reels and shorts blocks are off by default`() {
        assertFalse(AppSettings().shortFormOnlyBlockEnabled)
        assertFalse(AppSettings().shortFormAlwaysBlockEnabled)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
