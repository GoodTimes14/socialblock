package it.socialblock.domain.shortform

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortFormPanelsTest {
    @Test
    fun `only instagram youtube and x are supported`() {
        assertEquals(
            setOf("com.instagram.android", "com.google.android.youtube", "com.twitter.android"),
            ShortFormPanels.packageNames,
        )
        assertNull(ShortFormPanels.forPackage("com.example.social"))
    }

    @Test
    fun `only instagram and youtube can keep the app open at the limit`() {
        assertEquals(
            setOf("com.instagram.android", "com.google.android.youtube"),
            ShortFormPanels.limitPackageNames,
        )
    }

    @Test
    fun `panel view ids are fully qualified with their own package`() {
        ShortFormPanels.packageNames.forEach { packageName ->
            val viewIds = ShortFormPanels.forPackage(packageName)!!.viewIds

            assertTrue(viewIds.isNotEmpty())
            assertTrue(viewIds.all { it.startsWith("$packageName:id/") })
        }
    }

    @Test
    fun `full screen and split screen panels count but picture in picture does not`() {
        val shorts = ShortFormPanels.forPackage(ShortFormPanels.YOUTUBE_PACKAGE)!!

        // Sizes measured on a 1080x2424 emulator running YouTube Shorts.
        assertTrue(shorts.coversScreen(1080, 2093, SCREEN_WIDTH, SCREEN_HEIGHT))
        assertTrue(shorts.coversScreen(1080, 1100, SCREEN_WIDTH, SCREEN_HEIGHT))
        assertFalse(shorts.coversScreen(379, 673, SCREEN_WIDTH, SCREEN_HEIGHT))
        assertFalse(shorts.coversScreen(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT))
    }

    @Test
    fun `x counts a portrait video filling the screen but not a timeline video`() {
        val xVideos = ShortFormPanels.forPackage(ShortFormPanels.X_PACKAGE)!!

        assertTrue(xVideos.coversScreen(1080, 1920, SCREEN_WIDTH, SCREEN_HEIGHT))
        assertFalse(xVideos.coversScreen(1080, 1350, SCREEN_WIDTH, SCREEN_HEIGHT))
        assertFalse(xVideos.coversScreen(880, 1100, SCREEN_WIDTH, SCREEN_HEIGHT))
        assertFalse(xVideos.coversScreen(880, 495, SCREEN_WIDTH, SCREEN_HEIGHT))
    }

    @Test
    fun `accessibility service listens only to the supported packages`() {
        val config = File("src/main/res/xml/short_form_accessibility_service.xml").readText()
        val packageNames =
            Regex("""android:packageNames="([^"]*)"""")
                .find(config)!!
                .groupValues[1]
                .split(',')
                .toSet()

        assertEquals(ShortFormPanels.packageNames, packageNames)
    }

    private companion object {
        const val SCREEN_WIDTH = 1080
        const val SCREEN_HEIGHT = 2424
    }
}
