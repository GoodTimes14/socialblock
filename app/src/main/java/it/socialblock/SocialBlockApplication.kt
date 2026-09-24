package it.socialblock

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import it.socialblock.notifications.NotificationChannels
import it.socialblock.work.DailySummaryScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class SocialBlockApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var notificationChannels: NotificationChannels

    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notificationChannels.createAll()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            dailySummaryScheduler.scheduleFromSettings()
        }
    }
}
