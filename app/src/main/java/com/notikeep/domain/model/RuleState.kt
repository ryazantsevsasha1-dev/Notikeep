package com.notikeep.domain.model

/**
 * How Notikeep treats notifications from a given app. Exactly three states —
 * deliberately simpler than a rule builder (see RESEARCH.md, anti-pattern #5).
 */
enum class RuleState {
    /** Show in the shade AND keep in the archive. This is the default. */
    SHADE_AND_ARCHIVE,

    /** Silence from the shade but still keep in the archive. */
    ARCHIVE_ONLY,

    /** Do not capture at all. */
    IGNORE;

    /** Primary user decision: does this app's history get archived at all? */
    val saves: Boolean get() = this != IGNORE

    /** Secondary decision: do notifications still show in the shade? */
    val notifies: Boolean get() = this != ARCHIVE_ONLY

    companion object {
        /**
         * The UI exposes the rule as two toggles ("save?" + "notify?"); this maps
         * them back to the tri-state model. When saving is off the bell has no
         * meaning — we never intercept apps we ignore — so notify is disregarded.
         */
        fun from(save: Boolean, notify: Boolean): RuleState = when {
            !save -> IGNORE
            notify -> SHADE_AND_ARCHIVE
            else -> ARCHIVE_ONLY
        }
    }
}
