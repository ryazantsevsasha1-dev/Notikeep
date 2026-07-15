package com.notikeep.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.notikeep.data.local.dao.NotificationDao
import com.notikeep.data.local.toDomain
import com.notikeep.data.local.toEntity
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.model.DailyCounts
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
) : NotificationRepository {

    private fun pagingConfig() = PagingConfig(
        pageSize = 50,
        prefetchDistance = 25,
        enablePlaceholders = false,
    )

    override suspend fun save(record: NotificationRecord) {
        dao.insert(record.toEntity())
    }

    override suspend fun existsRecentByText(packageName: String, title: String, text: String, since: Long): Boolean =
        dao.existsRecentByText(packageName, title, text, since)

    override suspend fun existsRecentByTitle(packageName: String, title: String, since: Long): Boolean =
        dao.existsRecentByTitle(packageName, title, since)

    override suspend fun findBySbnKey(sbnKey: String): NotificationRecord? =
        dao.findBySbnKey(sbnKey)?.toDomain()

    override suspend fun update(record: NotificationRecord) = dao.update(record.toEntity())

    override fun observeAppSummaries(from: Long?, to: Long?): Flow<List<AppArchiveSummary>> =
        dao.observeAppSummaries(from ?: 0L, to ?: Long.MAX_VALUE)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeFavoriteAppSummaries(): Flow<List<AppArchiveSummary>> =
        dao.observeFavoriteAppSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun observeDailyCounts(startOfDayMillis: Long): Flow<DailyCounts> =
        dao.observeDailyCounts(startOfDayMillis)
            .map { DailyCounts(total = it.total, silenced = it.silenced ?: 0) }

    override fun pagedByPackage(packageName: String, favoritesOnly: Boolean): Flow<PagingData<NotificationRecord>> =
        Pager(pagingConfig()) {
            if (favoritesOnly) dao.pagingFavoritesByPackage(packageName)
            else dao.pagingByPackage(packageName)
        }.flow.map { data -> data.map { it.toDomain() } }

    override fun observeAppLabel(packageName: String): Flow<String?> =
        dao.observeAppLabel(packageName)

    override suspend fun markPackageRead(packageName: String) = dao.markPackageRead(packageName)

    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    override fun search(query: String, from: Long?, to: Long?): Flow<PagingData<NotificationRecord>> =
        Pager(pagingConfig()) {
            dao.pagingSearch(query.trim(), from ?: 0L, to ?: Long.MAX_VALUE)
        }.flow.map { data -> data.map { it.toDomain() } }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    override suspend fun deleteByPackage(packageName: String) = dao.deleteByPackage(packageName)

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun deleteOlderThan(olderThanMillis: Long): Int =
        dao.deleteOlderThan(olderThanMillis)
}
