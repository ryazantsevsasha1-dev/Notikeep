package com.notikeep.domain.usecase

import app.cash.turbine.test
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveArchiveUseCaseTest {

    private fun record(pkg: String, label: String, postedAt: Long) = NotificationRecord(
        id = postedAt,
        packageName = pkg,
        appLabel = label,
        title = "t",
        text = "x",
        postedAt = postedAt,
        wasSilenced = false,
    )

    @Test
    fun `groups by app and orders groups by latest activity`() = runTest {
        val repo = mockk<NotificationRepository>()
        every { repo.observeAll() } returns flowOf(
            listOf(
                record("a", "Alpha", 30),
                record("b", "Beta", 40),
                record("a", "Alpha", 10),
            ),
        )

        ObserveArchiveUseCase(repo)().test {
            val groups = awaitItem()
            assertEquals(2, groups.size)
            // Beta group is newest (40) so it comes first.
            assertEquals("Beta", groups[0].appLabel)
            assertEquals("Alpha", groups[1].appLabel)
            assertEquals(2, groups[1].notifications.size)
            awaitComplete()
        }
    }
}
