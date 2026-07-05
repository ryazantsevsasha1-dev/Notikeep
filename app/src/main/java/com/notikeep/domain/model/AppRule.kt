package com.notikeep.domain.model

/** The behaviour Notikeep applies to one app's notifications. */
data class AppRule(
    val packageName: String,
    val appLabel: String,
    val state: RuleState,
)
