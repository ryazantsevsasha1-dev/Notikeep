package com.notikeep.domain.usecase

import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.RuleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyRuleUseCaseTest {

    private val notifications = mockk<NotificationRepository>(relaxed = true)
    private val rules = mockk<RuleRepository>()
    private val useCase = ApplyRuleUseCase(notifications, rules)

    private val record = NotificationRecord(
        id = 0,
        packageName = "com.whatsapp",
        appLabel = "WhatsApp",
        title = "Hi",
        text = "Hello",
        postedAt = 1L,
        wasSilenced = false,
    )

    @Test
    fun `shade and archive keeps notification and saves it`() = runTest {
        coEvery { rules.stateFor(any()) } returns RuleState.SHADE_AND_ARCHIVE

        val action = useCase(record)

        assertEquals(ApplyRuleUseCase.ShadeAction.KEEP, action)
        coVerify { notifications.save(record) }
    }

    @Test
    fun `archive only cancels shade and saves as silenced`() = runTest {
        coEvery { rules.stateFor(any()) } returns RuleState.ARCHIVE_ONLY

        val action = useCase(record)

        assertEquals(ApplyRuleUseCase.ShadeAction.CANCEL, action)
        coVerify { notifications.save(record.copy(wasSilenced = true)) }
    }

    @Test
    fun `ignore neither saves nor cancels`() = runTest {
        coEvery { rules.stateFor(any()) } returns RuleState.IGNORE

        val action = useCase(record)

        assertEquals(ApplyRuleUseCase.ShadeAction.KEEP, action)
        coVerify(exactly = 0) { notifications.save(any()) }
    }
}
