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
    IGNORE,
}
