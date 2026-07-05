package com.notikeep.data.billing

import com.notikeep.domain.port.EntitlementProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Free build: everything is unlocked. When RuStore billing arrives, add a new
 * implementation of [EntitlementProvider] and rebind it in DI — no caller changes.
 */
class FreeEntitlementProvider @Inject constructor() : EntitlementProvider {
    override fun isPremium(): Flow<Boolean> = flowOf(true)
}
