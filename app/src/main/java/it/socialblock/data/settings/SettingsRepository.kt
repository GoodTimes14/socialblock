package it.socialblock.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.OverlayPosition
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "socialblock_settings")

@Singleton
class SettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    val settings: Flow<AppSettings> =
        context.settingsDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(
                        androidx.datastore.preferences.core.emptyPreferences(),
                    )
                } else {
                    throw error
                }
            }.map { preferences ->
                AppSettings(
                    notificationHour =
                        preferences[Keys.NOTIFICATION_HOUR] ?: DEFAULT_NOTIFICATION_HOUR,
                    notificationMinute = preferences[Keys.NOTIFICATION_MINUTE] ?: 0,
                    emergencyBypassUntilMillis = preferences[Keys.BYPASS_UNTIL] ?: 0L,
                    monitoringEnabled = preferences[Keys.MONITORING_ENABLED] ?: false,
                    overlayPosition =
                        OverlayPosition.fromStoredValue(
                            preferences[Keys.OVERLAY_POSITION],
                        ),
                    overlayOpacityPercent =
                        (
                            preferences[Keys.OVERLAY_OPACITY_PERCENT]
                                ?: AppSettings.DEFAULT_OVERLAY_OPACITY_PERCENT
                            )
                            .coerceIn(
                                AppSettings.MIN_OVERLAY_OPACITY_PERCENT,
                                AppSettings.MAX_OVERLAY_OPACITY_PERCENT,
                            ),
                    overlayMessages =
                        preferences[Keys.OVERLAY_MESSAGES]
                            ?.let(OverlayMessagesCodec::decode)
                            ?: AppSettings.DEFAULT_OVERLAY_MESSAGES,
                    overlayMessageChangeDelaySeconds =
                        (
                            preferences[Keys.OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS]
                                ?: AppSettings.DEFAULT_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS
                            )
                            .coerceIn(
                                AppSettings.MIN_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
                                AppSettings.MAX_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
                            ),
                )
            }

    suspend fun setDailySummaryTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.NOTIFICATION_HOUR] = hour.coerceIn(0, MAX_HOUR)
            preferences[Keys.NOTIFICATION_MINUTE] = minute.coerceIn(0, MAX_MINUTE)
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.MONITORING_ENABLED] = enabled
        }
    }

    suspend fun setOverlayPosition(position: OverlayPosition) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.OVERLAY_POSITION] = position.name
        }
    }

    suspend fun setOverlayOpacityPercent(percent: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.OVERLAY_OPACITY_PERCENT] =
                percent.coerceIn(
                    AppSettings.MIN_OVERLAY_OPACITY_PERCENT,
                    AppSettings.MAX_OVERLAY_OPACITY_PERCENT,
                )
        }
    }

    suspend fun addOverlayMessage(message: String) {
        updateOverlayMessages { messages -> messages + message }
    }

    suspend fun updateOverlayMessage(index: Int, message: String) {
        updateOverlayMessages { messages ->
            if (index !in messages.indices) {
                messages
            } else {
                messages.toMutableList().apply { this[index] = message }
            }
        }
    }

    suspend fun removeOverlayMessage(index: Int) {
        updateOverlayMessages { messages -> messages.filterIndexed { messageIndex, _ -> messageIndex != index } }
    }

    suspend fun setOverlayMessageChangeDelaySeconds(seconds: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS] =
                seconds.coerceIn(
                    AppSettings.MIN_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
                    AppSettings.MAX_OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS,
                )
        }
    }

    suspend fun enableEmergencyBypass(nowMillis: Long, durationMinutes: Int = DEFAULT_BYPASS_MINUTES): Long {
        val untilMillis = nowMillis + durationMinutes.coerceAtLeast(1) * MILLIS_PER_MINUTE
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.BYPASS_UNTIL] = untilMillis
        }
        return untilMillis
    }

    suspend fun clearEmergencyBypass() {
        context.settingsDataStore.edit { preferences -> preferences[Keys.BYPASS_UNTIL] = 0L }
    }

    private suspend fun updateOverlayMessages(transform: (List<String>) -> List<String>) {
        context.settingsDataStore.edit { preferences ->
            val current =
                preferences[Keys.OVERLAY_MESSAGES]
                    ?.let(OverlayMessagesCodec::decode)
                    ?: AppSettings.DEFAULT_OVERLAY_MESSAGES
            val updated =
                transform(current).mapNotNull { message ->
                    message.trim()
                        .take(AppSettings.MAX_OVERLAY_MESSAGE_LENGTH)
                        .takeIf(String::isNotEmpty)
                }
            preferences[Keys.OVERLAY_MESSAGES] = OverlayMessagesCodec.encode(updated)
        }
    }

    private object Keys {
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val BYPASS_UNTIL = longPreferencesKey("emergency_bypass_until")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
        val OVERLAY_OPACITY_PERCENT = intPreferencesKey("overlay_opacity_percent")
        val OVERLAY_MESSAGES = stringPreferencesKey("overlay_messages")
        val OVERLAY_MESSAGE_CHANGE_DELAY_SECONDS =
            intPreferencesKey("overlay_message_change_delay_seconds")
    }

    companion object {
        const val DEFAULT_BYPASS_MINUTES = 15
        private const val DEFAULT_NOTIFICATION_HOUR = 22
        private const val MAX_HOUR = 23
        private const val MAX_MINUTE = 59
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
