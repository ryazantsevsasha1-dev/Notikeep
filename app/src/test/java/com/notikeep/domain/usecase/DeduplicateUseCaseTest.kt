package com.notikeep.domain.usecase

import com.notikeep.domain.model.DedupStrategy
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicateUseCaseTest {

    private val repository = mockk<NotificationRepository>(relaxed = true)
    private val useCase = DeduplicateUseCase(repository).apply { now = { NOW } }

    private val record = NotificationRecord(
        id = 0,
        packageName = "org.telegram.messenger",
        appLabel = "Telegram",
        title = "Anna",
        text = "Hi there",
        postedAt = NOW,
        wasSilenced = false,
        sbnKey = "0|org.telegram.messenger|42|null|10123",
    )

    @Test
    fun `OFF always saves`() = runTest {
        val written = useCase(record, DedupStrategy.OFF)

        assertTrue(written)
        coVerify { repository.save(record) }
    }

    @Test
    fun `EXACT_TEXT_WINDOW skips when a recent identical row exists`() = runTest {
        coEvery { repository.countRecentByText(any(), any(), any(), any()) } returns 1

        val written = useCase(record, DedupStrategy.EXACT_TEXT_WINDOW)

        assertFalse(written)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `EXACT_TEXT_WINDOW saves when nothing recent matches`() = runTest {
        coEvery { repository.countRecentByText(any(), any(), any(), any()) } returns 0

        val written = useCase(record, DedupStrategy.EXACT_TEXT_WINDOW)

        assertTrue(written)
        coVerify { repository.save(record) }
    }

    @Test
    fun `EXACT_TEXT_WINDOW queries with the correct window boundary`() = runTest {
        coEvery { repository.countRecentByText(any(), any(), any(), any()) } returns 0

        useCase(record, DedupStrategy.EXACT_TEXT_WINDOW)

        coVerify {
            repository.countRecentByText(
                record.packageName, record.title, record.text,
                NOW - DedupStrategy.DEDUP_WINDOW_MS,
            )
        }
    }

    @Test
    fun `TITLE_ONLY_WINDOW skips on a recent same-title row`() = runTest {
        coEvery { repository.countRecentByTitle(any(), any(), any()) } returns 2

        val written = useCase(record, DedupStrategy.TITLE_ONLY_WINDOW)

        assertFalse(written)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `BY_KEY updates the existing row in place instead of inserting`() = runTest {
        val existing = record.copy(id = 7, text = "old", isFavorite = true)
        coEvery { repository.findBySbnKey(record.sbnKey!!) } returns existing

        val written = useCase(record, DedupStrategy.BY_KEY)

        assertTrue(written)
        coVerify {
            repository.update(
                existing.copy(
                    title = record.title,
                    text = record.text,
                    postedAt = record.postedAt,
                    wasSilenced = record.wasSilenced,
                ),
            )
        }
        coVerify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `BY_KEY inserts when there is no prior row for the key`() = runTest {
        coEvery { repository.findBySbnKey(any()) } returns null

        val written = useCase(record, DedupStrategy.BY_KEY)

        assertTrue(written)
        coVerify { repository.save(record) }
    }

    @Test
    fun `BY_KEY inserts when the record has no key`() = runTest {
        val keyless = record.copy(sbnKey = null)

        val written = useCase(keyless, DedupStrategy.BY_KEY)

        assertTrue(written)
        coVerify { repository.save(keyless) }
    }

    @Test
    fun `COMBINED collapses by key first`() = runTest {
        val existing = record.copy(id = 3)
        coEvery { repository.findBySbnKey(record.sbnKey!!) } returns existing

        val written = useCase(record, DedupStrategy.COMBINED)

        assertTrue(written)
        coVerify { repository.update(any()) }
        coVerify(exactly = 0) { repository.countRecentByText(any(), any(), any(), any()) }
    }

    @Test
    fun `COMBINED falls back to text window when key is absent`() = runTest {
        val keyless = record.copy(sbnKey = null)
        coEvery { repository.countRecentByText(any(), any(), any(), any()) } returns 1

        val written = useCase(keyless, DedupStrategy.COMBINED)

        assertFalse(written)
        coVerify(exactly = 0) { repository.save(any()) }
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
