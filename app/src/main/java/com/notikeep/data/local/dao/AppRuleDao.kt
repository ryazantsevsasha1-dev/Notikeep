package com.notikeep.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.notikeep.data.local.entity.AppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {

    @Upsert(entity = AppRuleEntity::class)
    suspend fun upsert(rule: AppRuleEntity)

    @Query("SELECT * FROM app_rules ORDER BY appLabel COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun findByPackage(packageName: String): AppRuleEntity?
}
