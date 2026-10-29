package it.socialblock.domain.shortform

import it.socialblock.domain.model.AppSettings
import javax.inject.Inject

enum class ShortFormBlockReason {
    ALWAYS,
    LIMIT_REACHED,
}

class ShortFormBlockPolicy @Inject constructor() {
    /**
     * Apps whose short-form panel must be closed now, and why. The always-on block covers every
     * supported app, with or without a timer; the emergency pause suspends both blocks.
     */
    fun blockedPackages(settings: AppSettings, nowMillis: Long, limitReachedPackages: Set<String>): Map<String, ShortFormBlockReason> =
        when {
            settings.isEmergencyBypassActive(nowMillis) -> emptyMap()
            settings.shortFormAlwaysBlockEnabled -> ShortFormPanels.packageNames.associateWith { ShortFormBlockReason.ALWAYS }
            else -> limitReachedPackages.associateWith { ShortFormBlockReason.LIMIT_REACHED }
        }
}
