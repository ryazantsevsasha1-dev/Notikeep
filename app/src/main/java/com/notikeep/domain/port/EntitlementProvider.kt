package com.notikeep.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * Billing boundary. The app is free today, so the only implementation unlocks
 * everything. When RuStore billing lands it becomes a new implementation behind
 * this same interface — features gate on [isPremium], never on a billing SDK.
 */
interface EntitlementProvider {
    fun isPremium(): Flow<Boolean>
}
