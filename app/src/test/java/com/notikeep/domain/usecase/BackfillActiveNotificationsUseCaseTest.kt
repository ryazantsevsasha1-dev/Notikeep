package com.notikeep.domain.usecase

import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.model.RuleState
import com.notikeep.domain.repository.NotificationRepository
import com.notikeep.domain.repository.RuleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BackfillActiveNotificationsUseCaseTest {

    private val notifications = mockk<NotificationRepository>(relaxed = true)
    private val rules = mockk<RuleRepository>()
    private val useCase = BackfillActiveNotificationsUseCase(notifications, rules)

    private fun record(pkg: String) = NotificationRecord(
        id = 0,
        packageName = pkg,
        appLabel = pkg,
        title = "t",
        text = "x",
        postedAt = 1L,
        wasSilenced = false,
    )

    @Test
    fun `saves records for apps that are not ignored`() = runTest {
        coEvery { rules.stateFor("kept") } returns RuleState.SHADE_AND_ARCHIVE
        coEvery { rules.stateFor("silenced") } returns RuleState.ARCHIVE_ONLY

        useCase(listOf(record("kept"), record("silenced")))

        coVerify { notifications.save(record("kept")) }
        coVerify { notifications.save(record("silenced")) }
    }

    @Test
    fun `skips ignored apps entirely`() = runTest {
        coEvery { rules.stateFor("ignored") } returns RuleState.IGNORE

        useCase(listOf(record("ignored")))

        coVerify(exactly = 0) { notifications.save(any()) }
    }

    @Test
    fun `empty input does nothing`() = runTest {
        useCase(emptyList())

        coVerify(exactly = 0) { notifications.save(any()) }
    }
}
