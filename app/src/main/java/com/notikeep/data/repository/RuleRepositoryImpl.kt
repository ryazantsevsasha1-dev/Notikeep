package com.notikeep.data.repository

import com.notikeep.data.local.dao.AppRuleDao
import com.notikeep.data.local.entity.AppRuleEntity
import com.notikeep.data.local.toDomain
import com.notikeep.domain.model.AppRule
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val dao: AppRuleDao,
) : RuleRepository {

    override fun observeAll(): Flow<List<AppRule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun stateFor(packageName: String): RuleState =
        dao.findByPackage(packageName)?.state ?: RuleState.SHADE_AND_ARCHIVE

    override suspend fun setState(packageName: String, appLabel: String, state: RuleState) =
        dao.upsert(AppRuleEntity(packageName, appLabel, state))
}
