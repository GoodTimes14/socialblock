package it.socialblock.di

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.socialblock.data.local.DailyUsageDao
import it.socialblock.data.local.SocialBlockDatabase
import it.socialblock.data.local.TrackedAppDao
import it.socialblock.platform.time.SystemTimeProvider
import it.socialblock.platform.time.TimeProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SocialBlockDatabase =
        Room.databaseBuilder(context, SocialBlockDatabase::class.java, "socialblock.db").build()

    @Provides
    fun provideTrackedAppDao(database: SocialBlockDatabase): TrackedAppDao = database.trackedAppDao()

    @Provides
    fun provideDailyUsageDao(database: SocialBlockDatabase): DailyUsageDao = database.dailyUsageDao()

    @Provides
    fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager =
        context.getSystemService(UsageStatsManager::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindTimeProvider(implementation: SystemTimeProvider): TimeProvider
}
