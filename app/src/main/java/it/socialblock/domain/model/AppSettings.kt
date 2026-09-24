package it.socialblock.domain.model

data class AppSettings(
    val notificationHour: Int = 22,
    val notificationMinute: Int = 0,
    val emergencyBypassUntilMillis: Long = 0L,
    val monitoringEnabled: Boolean = false,
    val overlayPosition: OverlayPosition = OverlayPosition.CENTER,
    val overlayOpacityPercent: Int = DEFAULT_OVERLAY_OPACITY_PERCENT,
    val overlayMessages: List<String> = DEFAULT_OVERLAY_MESSAGES,
    val overlayMessageChangeDelaySeconds: Int = DEFAULT_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
) {
    fun isEmergencyBypassActive(nowMillis: Long): Boolean = emergencyBypassUntilMillis > nowMillis

    val overlayOpacity: Float
        get() =
            overlayOpacityPercent.coerceIn(
                MIN_OVERLAY_OPACITY_PERCENT,
                MAX_OVERLAY_OPACITY_PERCENT,
            ) / PERCENT_SCALE

    companion object {
        const val DEFAULT_OVERLAY_OPACITY_PERCENT = 78
        const val MIN_OVERLAY_OPACITY_PERCENT = 20
        const val MAX_OVERLAY_OPACITY_PERCENT = 80
        const val DEFAULT_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS = 30
        const val MIN_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS = 5
        const val MAX_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS = 3_600
        const val MAX_OVERLAY_MESSAGE_LENGTH = 240
        private const val PERCENT_SCALE = 100f

        val DEFAULT_OVERLAY_MESSAGES =
            listOf(
                "I social media provocano dipendenza, smetti subito",
                "I social media riducono la tua soglia di attenzione",
                "L'abuso dei social media può renderti facilmente manipolabile",
                "Il tempo che stai spendendo qui non tornerà",
            )
    }
}
