package com.notikeep.domain.model

/** How many notifications were captured today, and how many of those were silenced. */
data class DailyCounts(val total: Int, val silenced: Int) {
    companion object {
        val EMPTY = DailyCounts(0, 0)
    }
}
