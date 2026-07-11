package com.notikeep.domain.usecase

import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.repository.RuleRepository
import com.notikeep.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Decides what happens to one incoming notification based on its app's rule, and
 * archives it if needed. Pure enough to unit-test without Android: the platform
 * side-effect (cancelling from the shade) is returned as a decision, not done here.
 *
 * Archiving goes through [DeduplicateUseCase], which suppresses spammy repeats
 * according to the user's chosen strategy.
 */
class ApplyRuleUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val deduplicate: DeduplicateUseCase,
    private val settingsRepository: SettingsRepository,
) {
    /** What the caller (the listener service) should do after this use case runs. */
    enum class ShadeAction { KEEP, CANCEL }

    suspend operator fun invoke(record: NotificationRecord): ShadeAction {
        return when (ruleRepository.stateFor(record.packageName)) {
            RuleState.IGNORE -> ShadeAction.KEEP // not captured, leave the shade untouched
            RuleState.SHADE_AND_ARCHIVE -> {
                archive(record)
                ShadeAction.KEEP
            }
            RuleState.ARCHIVE_ONLY -> {
                archive(record.copy(wasSilenced = true))
                ShadeAction.CANCEL
            }
        }
    }

    private suspend fun archive(record: NotificationRecord) {
        val strategy = settingsRepository.observe().first().dedupStrategy
        deduplicate(record, strategy)
    }
}
