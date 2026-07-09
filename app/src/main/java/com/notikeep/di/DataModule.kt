package com.notikeep.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.notikeep.data.local.NotikeepDatabase
import com.notikeep.data.local.dao.AppRuleDao
import com.notikeep.data.local.dao.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "notikeep_settings")

/** Provides framework-level singletons: database, DAOs, DataStore, app-wide scope. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotikeepDatabase =
        Room.databaseBuilder(context, NotikeepDatabase::class.java, NotikeepDatabase.NAME)
            .addMigrations(
                NotikeepDatabase.MIGRATION_1_2,
                NotikeepDatabase.MIGRATION_2_3,
                NotikeepDatabase.MIGRATION_3_4,
            )
            .build()

    @Provides
    fun provideNotificationDao(db: NotikeepDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideAppRuleDao(db: NotikeepDatabase): AppRuleDao = db.appRuleDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    /**
     * Long-lived scope for fire-and-forget work in services/analytics. The
     * exception handler makes failures visible: a swallowed DB error here would
     * otherwise mean silently lost notifications.
     */
    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("NotikeepAppScope", "background task failed", throwable)
        },
    )
}
