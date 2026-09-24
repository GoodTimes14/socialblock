package it.socialblock.monitoring

import it.socialblock.domain.model.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayMessageProvider @Inject constructor() {
    fun message(packageName: String, nowMillis: Long, settings: AppSettings): String? {
        if (settings.overlayMessages.isEmpty()) return null
        val delayMillis =
            settings.overlayMessageChangeDelaySeconds.coerceIn(
                AppSettings.MIN_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
                AppSettings.MAX_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
            ) * MILLIS_PER_SECOND
        val rotation = Math.floorDiv(nowMillis, delayMillis)
        val index =
            Math.floorMod(
                packageName.hashCode().toLong() + rotation,
                settings.overlayMessages.size.toLong(),
            ).toInt()
        return settings.overlayMessages[index]
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
