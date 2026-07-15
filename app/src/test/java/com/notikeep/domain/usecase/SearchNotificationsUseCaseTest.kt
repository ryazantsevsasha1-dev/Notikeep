package com.notikeep.domain.usecase

import androidx.paging.PagingData
import app.cash.turbine.test
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchNotificationsUseCaseTest {

    private val repo = mockk<NotificationRepository>(relaxed = true)
    private val useCase = SearchNotificationsUseCase(repo)

    @Test
    fun `blank query emits once without hitting the repository`() = runTest {
        useCase("   ").test {
            awaitItem() // PagingData.empty()
            awaitComplete()
        }
        verify(exactly = 0) { repo.search(any(), any(), any()) }
    }

    @Test
    fun `non-blank query is trimmed and delegated`() = runTest {
        every { repo.search(any(), any(), any()) } returns
            flowOf(PagingData.empty<NotificationRecord>())
        useCase("  promo  ").test {
            awaitItem()
            awaitComplete()
        }
        verify { repo.search("promo", null, null) }
    }
}
