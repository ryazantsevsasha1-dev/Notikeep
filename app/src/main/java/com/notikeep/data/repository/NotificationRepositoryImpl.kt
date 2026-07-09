package com.notikeep.data.repository

import com.notikeep.data.local.dao.NotificationDao
import com.notikeep.data.local.toDomain
import com.notikeep.data.local.toEntity
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
) : NotificationRepository {

    override suspend fun save(record: NotificationRecord) {
        dao.insert(record.toEntity())
    }

    override fun observeAll(): Flow<List<NotificationRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAppSummaries(from: Long?, to: Long?): Flow<List<AppArchiveSummary>> =
        dao.observeAppSummaries(from ?: 0L, to ?: Long.MAX_VALUE)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeFavoriteAppSummaries(): Flow<List<AppArchiveSummary>> =
        dao.observeFavoriteAppSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun observeByPackage(packageName: String): Flow<List<NotificationRecord>> =
        dao.observeByPackage(packageName).map { list -> list.map { it.toDomain() } }

    override fun observeFavoritesByPackage(packageName: String): Flow<List<NotificationRecord>> =
        dao.observeFavoritesByPackage(packageName).map { list -> list.map { it.toDomain() } }

    override suspend fun markPackageRead(packageName: String) = dao.markPackageRead(packageName)

    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    override fun search(query: String, from: Long?, to: Long?): Flow<List<NotificationRecord>> =
        dao.search(query.trim(), from ?: 0L, to ?: Long.MAX_VALUE)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun deleteOlderThan(olderThanMillis: Long): Int =
        dao.deleteOlderThan(olderThanMillis)
}
