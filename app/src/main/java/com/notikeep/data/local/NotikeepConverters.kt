package com.notikeep.data.local

import androidx.room.TypeConverter
import com.notikeep.domain.model.RuleState

/** Persists domain enums as stable string names, resilient to reordering. */
class NotikeepConverters {

    @TypeConverter
    fun ruleStateToString(state: RuleState): String = state.name

    @TypeConverter
    fun stringToRuleState(value: String): RuleState = RuleState.valueOf(value)
}
