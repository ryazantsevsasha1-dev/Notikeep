package com.notikeep.domain.port

import com.notikeep.domain.model.InstalledApp

/**
 * Read-only view of the launchable apps on the device. Abstacts PackageManager
 * away from presentation/domain so ViewModels never touch data-layer concretes.
 */
interface AppCatalog {
    suspend fun listLaunchable(forceRefresh: Boolean = false): List<InstalledApp>
}
