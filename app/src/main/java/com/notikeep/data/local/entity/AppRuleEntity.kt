package com.notikeep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notikeep.domain.model.RuleState

/** Room row for a per-app rule. Only apps the user changed are stored. */
@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val state: RuleState,
)
