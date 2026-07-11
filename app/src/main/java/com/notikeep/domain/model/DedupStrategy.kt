package com.notikeep.domain.model

/**
 * How aggressively to suppress duplicate/spammy captures. Exposed in settings as
 * an experiment: apps like VPNs or Telegram re-post or update notifications
 * constantly, flooding the archive with near-identical copies. Each strategy is a
 * different trade-off between catching those and keeping legitimate repeats.
 */
enum class DedupStrategy {
    /** No suppression — store every captured notification (legacy behaviour). */
    OFF,

    /** Skip if the same app posted the identical title+text within [DEDUP_WINDOW_MS]. */
    EXACT_TEXT_WINDOW,

    /** Skip if the same app posted the identical title within [DEDUP_WINDOW_MS] (text may drift). */
    TITLE_ONLY_WINDOW,

    /** Collapse updates of the same OS notification (same sbnKey) onto one row. */
    BY_KEY,

    /** BY_KEY plus EXACT_TEXT_WINDOW: collapse updates and drop rapid identical reposts. */
    COMBINED;

    companion object {
        val DEFAULT = OFF

        /** Time window for the "recent duplicate" strategies. */
        const val DEDUP_WINDOW_MS = 10_000L

        fun fromNameOrDefault(name: String?): DedupStrategy =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
