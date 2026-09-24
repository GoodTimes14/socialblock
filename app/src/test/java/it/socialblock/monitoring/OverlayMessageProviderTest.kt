package it.socialblock.monitoring

import it.socialblock.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayMessageProviderTest {
    private val provider = OverlayMessageProvider()

    @Test
    fun `uses configured messages and change delay`() {
        val settings =
            AppSettings(
                overlayMessages = listOf("Primo", "Secondo"),
                overlayMessageChangeDelaySeconds = 10,
            )

        val initial = provider.message(PACKAGE_NAME, 0L, settings)

        assertEquals(initial, provider.message(PACKAGE_NAME, 9_999L, settings))
        assertNotEquals(initial, provider.message(PACKAGE_NAME, 10_000L, settings))
    }

    @Test
    fun `returns no message when the configured list is empty`() {
        val settings = AppSettings(overlayMessages = emptyList())

        assertNull(provider.message(PACKAGE_NAME, 0L, settings))
    }

    private companion object {
        const val PACKAGE_NAME = "com.example.social"
    }
}
