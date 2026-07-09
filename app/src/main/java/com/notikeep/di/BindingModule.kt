package com.notikeep.di

import com.notikeep.data.analytics.AppMetricaAnalytics
import com.notikeep.data.billing.FreeEntitlementProvider
import com.notikeep.data.repository.NotificationRepositoryImpl
import com.notikeep.data.repository.RuleRepositoryImpl
import com.notikeep.data.service.InstalledAppsProvider
import com.notikeep.data.service.UsageStatsProvider
import com.notikeep.data.settings.SettingsRepositoryImpl
import com.notikeep.domain.port.Analytics
import com.notikeep.domain.port.AppCatalog
import com.notikeep.domain.port.AppUsageStats
import com.notikeep.domain.port.EntitlementProvider
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.RuleRepository
import com.notikeep.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain abstractions to their implementations. Swapping an implementation
 * (e.g. analytics or billing) is a one-line change here — the "fix by OOP, not
 * by hack" rule in ARCHITECTURE.md.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAnalytics(impl: AppMetricaAnalytics): Analytics

    @Binds
    @Singleton
    abstract fun bindEntitlementProvider(impl: FreeEntitlementProvider): EntitlementProvider

    @Binds
    @Singleton
    abstract fun bindAppCatalog(impl: InstalledAppsProvider): AppCatalog

    @Binds
    @Singleton
    abstract fun bindAppUsageStats(impl: UsageStatsProvider): AppUsageStats
}
