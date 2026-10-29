package it.socialblock.monitoring

import it.socialblock.data.settings.SettingsRepository
import it.socialblock.domain.shortform.ShortFormBlockPolicy
import it.socialblock.domain.shortform.ShortFormBlockReason
import it.socialblock.platform.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/** Shared between the monitoring service, which tracks the limits, and the accessibility service, which closes the panels. */
@Singleton
class ShortFormBlockState @Inject constructor(
    settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val policy: ShortFormBlockPolicy,
) {
    private val mutableLimitBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val mutableBlockerConnected = MutableStateFlow(false)

    val blockerConnected: StateFlow<Boolean> = mutableBlockerConnected.asStateFlow()

    /** Apps whose short-form panel must be closed now, and why. Re-evaluated when the emergency pause ends. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val blockedPackages: Flow<Map<String, ShortFormBlockReason>> =
        combine(settingsRepository.settings, mutableLimitBlockedPackages, ::Pair)
            .transformLatest { (settings, limitBlockedPackages) ->
                var bypassLeftMillis = settings.emergencyBypassUntilMillis - timeProvider.nowMillis()
                while (bypassLeftMillis > 0) {
                    emit(emptyMap())
                    // delay() stands still while the device sleeps, so the clock is checked again often.
                    delay(minOf(bypassLeftMillis, BYPASS_RECHECK_MILLIS))
                    bypassLeftMillis = settings.emergencyBypassUntilMillis - timeProvider.nowMillis()
                }
                emit(policy.blockedPackages(settings, timeProvider.nowMillis(), limitBlockedPackages))
            }.distinctUntilChanged()

    /** Apps at their limit whose Reels/Shorts, rather than the whole app, must be closed. */
    fun updateLimitBlockedPackages(packageNames: Set<String>) {
        mutableLimitBlockedPackages.value = packageNames
    }

    fun setBlockerConnected(connected: Boolean) {
        mutableBlockerConnected.value = connected
    }

    private companion object {
        const val BYPASS_RECHECK_MILLIS = 5_000L
    }
}
