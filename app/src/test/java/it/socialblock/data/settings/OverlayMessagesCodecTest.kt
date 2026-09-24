package it.socialblock.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayMessagesCodecTest {
    @Test
    fun `round trips message text without reserving characters`() {
        val messages =
            listOf(
                "Due punti: e numeri 123",
                "Un messaggio\nsu due righe",
                "Emoji 📵",
            )

        assertEquals(messages, OverlayMessagesCodec.decode(OverlayMessagesCodec.encode(messages)))
    }

    @Test
    fun `preserves an intentionally empty list`() {
        assertEquals(emptyList<String>(), OverlayMessagesCodec.decode(OverlayMessagesCodec.encode(emptyList())))
    }

    @Test
    fun `rejects corrupted stored data`() {
        assertNull(OverlayMessagesCodec.decode("999999999:testo"))
        assertNull(OverlayMessagesCodec.decode("senza-lunghezza"))
    }
}
