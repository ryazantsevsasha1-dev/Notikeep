package com.notikeep.domain.repository

import com.notikeep.domain.model.AppRule
import com.notikeep.domain.model.RuleState
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for per-app rules. */
interface RuleRepository {

    fun observeAll(): Flow<List<AppRule>>

    /**
     * The effective state for a package. Apps without an explicit rule default
     * to [RuleState.SHADE_AND_ARCHIVE] so nothing is silently lost.
     */
    suspend fun stateFor(packageName: String): RuleState

    suspend fun setState(packageName: String, appLabel: String, state: RuleState)
}
