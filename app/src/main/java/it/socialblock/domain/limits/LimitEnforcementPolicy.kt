package it.socialblock.domain.limits

import it.socialblock.domain.shortform.ShortFormFeature
import it.socialblock.domain.shortform.ShortFormPanels
import javax.inject.Inject

sealed interface LimitEnforcement {
    data object ReturnHome : LimitEnforcement

    data class BlockShortForm(val feature: ShortFormFeature) : LimitEnforcement
}

class LimitEnforcementPolicy @Inject constructor() {
    fun enforcementFor(
        packageName: String,
        shortFormOnlyBlockEnabled: Boolean,
        shortFormAlwaysBlockEnabled: Boolean,
        shortFormBlockerConnected: Boolean,
    ): LimitEnforcement {
        val feature = ShortFormPanels.forPackage(packageName)?.takeIf { packageName in ShortFormPanels.limitPackageNames }
        // With the always-on block Reels/Shorts are already closed, so the limit closes the whole app.
        val keepAppOpen = shortFormOnlyBlockEnabled && !shortFormAlwaysBlockEnabled
        // Without the accessibility service Reels/Shorts can't be closed, so the whole app stays limited.
        return if (keepAppOpen && shortFormBlockerConnected && feature != null) {
            LimitEnforcement.BlockShortForm(feature)
        } else {
            LimitEnforcement.ReturnHome
        }
    }
}
