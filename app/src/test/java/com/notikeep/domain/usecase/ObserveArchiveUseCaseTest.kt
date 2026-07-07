package com.notikeep.domain.usecase

import app.cash.turbine.test
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveArchiveUseCaseTest {

    private fun summary(pkg: String, label: String, lastPostedAt: Long, unread: Int) =
        AppArchiveSummary(
            packageName = pkg,
            appLabel = label,
            previewTitle = "t",
            previewText = "x",
            lastPostedAt = lastPostedAt,
            unreadCount = unread,
            totalCount = unread,
        )

    @Test
    fun `delegates to repository summaries with the given range`() = runTest {
        val repo = mockk<NotificationRepository>()
        every { repo.observeAppSummaries(10L, 20L) } returns flowOf(
            listOf(summary("b", "Beta", 40, 2), summary("a", "Alpha", 30, 0)),
        )

        ObserveArchiveUseCase(repo)(10L, 20L).test {
            val groups = awaitItem()
            assertEquals(2, groups.size)
            assertEquals("Beta", groups[0].appLabel)
            assertEquals(2, groups[0].unreadCount)
            awaitComplete()
        }
        verify { repo.observeAppSummaries(10L, 20L) }
    }
}
