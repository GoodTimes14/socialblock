package it.socialblock.monitoring

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import it.socialblock.data.repository.TrackingRepository
import it.socialblock.data.settings.SettingsRepository
import it.socialblock.domain.limits.LimitEvaluator
import it.socialblock.domain.limits.LimitStatus
import it.socialblock.domain.model.AppSettings
import it.socialblock.domain.model.TrackedApp
import it.socialblock.notifications.AppNotifier
import it.socialblock.platform.time.TimeProvider
import it.socialblock.usage.AndroidUsageStatsSource
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UsageMonitoringService : Service() {
    @Inject lateinit var trackingRepository: TrackingRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var usageStatsSource: AndroidUsageStatsSource

    @Inject lateinit var evaluator: LimitEvaluator

    @Inject lateinit var overlayController: WarningOverlayController

    @Inject lateinit var overlayMessageProvider: OverlayMessageProvider

    @Inject lateinit var limitEnforcer: LimitEnforcer

    @Inject lateinit var notifier: AppNotifier

    @Inject lateinit var timeProvider: TimeProvider

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private var lastSnapshotSavedAt = 0L
    private var lastNotificationState = MonitoringNotificationState(0L, null)

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitoringJob?.isActive != true) {
            monitoringJob = serviceScope.launch { monitoringLoop() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.launch { overlayController.hide() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        ServiceCompat.startForeground(
            this,
            AppNotifier.MONITORING_NOTIFICATION_ID,
            notifier.monitoringNotification(0L, null),
            serviceType,
        )
    }

    private suspend fun monitoringLoop() {
        combine(
            settingsRepository.settings,
            trackingRepository.observeTrackedApps(),
        ) { settings, apps -> MonitoringConfiguration(settings, apps) }
            .distinctUntilChanged()
            .collectLatest { configuration ->
                if (!configuration.settings.monitoringEnabled || configuration.apps.isEmpty()) {
                    overlayController.hide()
                    stopSelf()
                    return@collectLatest
                }

                while (currentCoroutineContext().isActive) {
                    val trackedAppInForeground =
                        try {
                            monitorOnce(configuration.settings, configuration.apps)
                        } catch (_: SecurityException) {
                            overlayController.hide()
                            false
                        } catch (_: RuntimeException) {
                            overlayController.hide()
                            false
                        }
                    delay(
                        if (trackedAppInForeground) {
                            ACTIVE_APP_POLL_INTERVAL_MILLIS
                        } else {
                            IDLE_POLL_INTERVAL_MILLIS
                        },
                    )
                }
            }
    }

    private suspend fun monitorOnce(settings: AppSettings, apps: List<TrackedApp>): Boolean {
        val nowMillis = timeProvider.nowMillis()
        val snapshot = usageStatsSource.snapshot(apps.map { it.packageName }.toSet(), nowMillis)
        val bypassActive = settings.isEmergencyBypassActive(nowMillis)
        val localDate = localDate(nowMillis)

        if (
            lastSnapshotSavedAt == 0L ||
            nowMillis < lastSnapshotSavedAt ||
            nowMillis - lastSnapshotSavedAt >= SNAPSHOT_PERSIST_INTERVAL_MILLIS
        ) {
            trackingRepository.saveUsageSnapshot(localDate, snapshot.usageByPackage, nowMillis)
            lastSnapshotSavedAt = nowMillis
        }

        apps.forEach { app ->
            val usedMillis = snapshot.usageByPackage[app.packageName] ?: 0L
            when (val status = evaluator.evaluate(app, usedMillis, bypassActive)) {
                is LimitStatus.Approaching -> {
                    if (app.lastApproachingNoticeDate != localDate) {
                        notifier.approachingLimit(app, status.remainingMillis)
                        trackingRepository.markApproachingNotice(app.packageName, localDate)
                    }
                }

                LimitStatus.Reached -> {
                    if (app.lastLimitNoticeDate != localDate) {
                        notifier.limitReached(app)
                        trackingRepository.markLimitNotice(app.packageName, localDate)
                    }
                }

                LimitStatus.Available,
                LimitStatus.Bypassed,
                -> Unit
            }
        }

        val foregroundApp = apps.firstOrNull { it.packageName == snapshot.foregroundPackage }
        when {
            bypassActive || foregroundApp == null -> overlayController.hide()

            evaluator.evaluate(
                foregroundApp,
                snapshot.usageByPackage[foregroundApp.packageName] ?: 0L,
                bypassActive = false,
            ) == LimitStatus.Reached -> {
                overlayController.hide()
                limitEnforcer.returnToHome()
            }

            foregroundApp.overlayEnabled -> {
                val message =
                    overlayMessageProvider.message(
                        foregroundApp.packageName,
                        nowMillis,
                        settings,
                    )
                if (message == null) {
                    overlayController.hide()
                } else {
                    overlayController.show(
                        message = message,
                        position = settings.overlayPosition,
                        opacity = settings.overlayOpacity,
                    )
                }
            }

            else -> overlayController.hide()
        }

        val notificationState =
            MonitoringNotificationState(
                usageBucket =
                    snapshot.usageByPackage.values.sum() /
                        NOTIFICATION_USAGE_BUCKET_MILLIS,
                bypassUntilMillis =
                    settings.emergencyBypassUntilMillis.takeIf { bypassActive },
            )
        if (notificationState != lastNotificationState) {
            val notification =
                notifier.monitoringNotification(
                    totalUsageMillis = snapshot.usageByPackage.values.sum(),
                    bypassUntilMillis = settings.emergencyBypassUntilMillis.takeIf { bypassActive },
                )
            getSystemService(android.app.NotificationManager::class.java)
                .notify(AppNotifier.MONITORING_NOTIFICATION_ID, notification)
            lastNotificationState = notificationState
        }
        return foregroundApp != null
    }

    private fun localDate(nowMillis: Long): String = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    private data class MonitoringConfiguration(val settings: AppSettings, val apps: List<TrackedApp>)

    private data class MonitoringNotificationState(val usageBucket: Long, val bypassUntilMillis: Long?)

    companion object {
        private const val ACTIVE_APP_POLL_INTERVAL_MILLIS = 2_000L
        private const val IDLE_POLL_INTERVAL_MILLIS = 5_000L
        private const val SNAPSHOT_PERSIST_INTERVAL_MILLIS = 5 * 60_000L
        private const val NOTIFICATION_USAGE_BUCKET_MILLIS = 5 * 60_000L
    }
}
