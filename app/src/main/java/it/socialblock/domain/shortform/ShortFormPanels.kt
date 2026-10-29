package it.socialblock.domain.shortform

/**
 * A short-form video panel, recognised only by the resource ID of the view that hosts it.
 * [minScreenShare] is how much of the screen that view must cover to count as the open panel.
 */
data class ShortFormFeature(val name: String, val viewIds: List<String>, val minScreenShare: Double = DEFAULT_MIN_SCREEN_SHARE) {
    /**
     * A picture-in-picture panel must not count: Back would reach the app in the foreground, not the
     * small window. A full-screen or split-screen panel covers far more than the default share.
     */
    fun coversScreen(panelWidth: Int, panelHeight: Int, screenWidth: Int, screenHeight: Int): Boolean =
        panelWidth.toLong() * panelHeight >= minScreenShare * screenWidth.toLong() * screenHeight

    private companion object {
        const val DEFAULT_MIN_SCREEN_SHARE = 0.3
    }
}

/**
 * Reels, Shorts and X's vertical video feed.
 * Keep the package list in sync with `res/xml/short_form_accessibility_service.xml`.
 */
object ShortFormPanels {
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val X_PACKAGE = "com.twitter.android"

    // X draws its screens with Compose, so only its video player keeps a view ID. Timeline and
    // post videos are smaller than this share; a portrait video in the full-screen feed covers more.
    private const val X_FEED_MIN_SCREEN_SHARE = 0.6

    private val featuresByPackage =
        mapOf(
            INSTAGRAM_PACKAGE to ShortFormFeature(
                name = "Reels",
                viewIds = viewIds(INSTAGRAM_PACKAGE, "clips_viewer_view_pager"),
            ),
            YOUTUBE_PACKAGE to ShortFormFeature(
                name = "Shorts",
                viewIds = viewIds(YOUTUBE_PACKAGE, "reel_recycler", "reel_player_page_container"),
            ),
            X_PACKAGE to ShortFormFeature(
                name = "Video di X",
                viewIds = viewIds(X_PACKAGE, "exo_content_frame"),
                minScreenShare = X_FEED_MIN_SCREEN_SHARE,
            ),
        )

    /** Every app whose short-form panel can be closed: all of them are closed by the always-on block. */
    val packageNames: Set<String> = featuresByPackage.keys

    /** Apps whose daily limit can close just the short-form panel instead of the whole app. */
    val limitPackageNames: Set<String> = setOf(INSTAGRAM_PACKAGE, YOUTUBE_PACKAGE)

    fun forPackage(packageName: String): ShortFormFeature? = featuresByPackage[packageName]

    private fun viewIds(packageName: String, vararg names: String): List<String> = names.map { "$packageName:id/$it" }
}
