package com.notikeep.domain.usecase

import app.cash.turbine.test
import com.notikeep.domain.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNotificationsUseCaseTest {

    private val repo = mockk<NotificationRepository>(relaxed = true)
    private val useCase = SearchNotificationsUseCase(repo)

    @Test
    fun `blank query returns empty without hitting the repository`() = runTest {
        useCase("   ").test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
        verify(exactly = 0) { repo.search(any()) }
    }

    @Test
    fun `non-blank query is trimmed and delegated`() = runTest {
        every { repo.search(any()) } returns flowOf(emptyList())
        useCase("  promo  ").test {
            awaitItem()
            awaitComplete()
        }
        verify { repo.search("promo") }
    }
}
