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

    override suspend fun save(record: NotificationRecord) = dao.insert(record.toEntity())

    override fun observeAll(): Flow<List<NotificationRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAppSummaries(from: Long?, to: Long?): Flow<List<AppArchiveSummary>> =
        dao.observeAppSummaries(from ?: 0L, to ?: Long.MAX_VALUE).map { rows ->
            rows.map {
                AppArchiveSummary(
                    packageName = it.packageName,
                    appLabel = it.appLabel,
                    previewTitle = it.previewTitle,
                    previewText = it.previewText,
                    lastPostedAt = it.lastPostedAt,
                    unreadCount = it.unreadCount,
                    totalCount = it.totalCount,
                )
            }
        }

    override fun observeByPackage(packageName: String): Flow<List<NotificationRecord>> =
        dao.observeByPackage(packageName).map { list -> list.map { it.toDomain() } }

    override suspend fun markPackageRead(packageName: String) = dao.markPackageRead(packageName)

    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    override fun search(query: String, from: Long?, to: Long?): Flow<List<NotificationRecord>> =
        dao.search(toFtsQuery(query), from ?: 0L, to ?: Long.MAX_VALUE)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun deleteOlderThan(olderThanMillis: Long): Int =
        dao.deleteOlderThan(olderThanMillis)

    /**
     * Turns free user text into a safe FTS4 prefix query. Each term is quoted to
     * neutralise FTS operators and gets a `*` for prefix matching.
     */
    private fun toFtsQuery(raw: String): String =
        raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "\"${it.replace("\"", "")}\"*" }
}
