package com.notikeep.domain.usecase

import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.RuleRepository
import javax.inject.Inject

/**
 * Archives the notifications already sitting in the shade at the moment the
 * listener connects (first launch or reconnect). Unlike [ApplyRuleUseCase] it
 * never asks to cancel anything: backfill must not touch the user's shade.
 *
 * Idempotent by design — the storage dedups identical records, so reconnecting
 * ten times a day stays safe.
 */
class BackfillActiveNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val ruleRepository: RuleRepository,
) {
    suspend operator fun invoke(records: List<NotificationRecord>) {
        for (record in records) {
            if (ruleRepository.stateFor(record.packageName) == RuleState.IGNORE) continue
            notificationRepository.save(record)
        }
    }
}
