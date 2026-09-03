package com.swipeclean.app.data.repository

import com.swipeclean.app.data.local.dao.ReviewedMediaDao
import com.swipeclean.app.data.local.dao.SessionStatsDao
import com.swipeclean.app.data.local.entity.ReviewedMediaEntity
import com.swipeclean.app.data.local.entity.SessionStatsEntity
import com.swipeclean.app.data.mediastore.MediaStoreDataSource
import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.domain.model.MediaMonthGroup
import com.swipeclean.app.domain.model.MediaQuery
import com.swipeclean.app.domain.model.ReviewDecision
import com.swipeclean.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val reviewedMediaDao: ReviewedMediaDao,
    private val sessionStatsDao: SessionStatsDao,
) : MediaRepository {

    override fun getBuckets(query: MediaQuery): Flow<List<MediaBucket>> =
        mediaStore.observeMediaChanges()
            .onStart { emit(Unit) }
            .map { mediaStore.getBuckets(query, reviewedMediaDao.getAllReviewedIds()) }
            .flowOn(Dispatchers.IO)

    override fun getMonthGroups(query: MediaQuery): Flow<List<MediaMonthGroup>> =
        mediaStore.observeMediaChanges()
            .onStart { emit(Unit) }
            .map { mediaStore.getMonthGroups(query, reviewedMediaDao.getAllReviewedIds()) }
            .flowOn(Dispatchers.IO)

    override fun observeMarkedForDeletion(): Flow<List<MediaItem>> =
        reviewedMediaDao.observeByDecision(ReviewDecision.DELETED)
            .map { rows ->
                val porId = mediaStore.getItemsByIds(rows.map { it.mediaId }).associateBy { it.id }
                // Se respeta el orden del historial (más reciente primero) y se
                // descartan las que ya no existen en MediaStore.
                rows.mapNotNull { porId[it.mediaId] }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun presentMediaIds(mediaIds: List<Long>): Set<Long> {
        if (mediaIds.isEmpty()) return emptySet()
        return mediaStore.getItemsByIds(mediaIds).mapTo(HashSet()) { it.id }
    }

    override suspend fun forgetReviewed(mediaIds: List<Long>) {
        if (mediaIds.isNotEmpty()) reviewedMediaDao.deleteByIds(mediaIds)
    }

    override suspend fun getMediaPage(query: MediaQuery, offset: Int, limit: Int): List<MediaItem> {
        // El filtrado de revisados ocurre en la consulta a MediaStore (cláusula
        // `_ID NOT IN (...)`), no recorriendo la página en memoria.
        val reviewedIds = reviewedMediaDao.getAllReviewedIds()
        return mediaStore.getPage(query, offset, limit, reviewedIds)
    }

    override suspend fun countMedia(query: MediaQuery): Int {
        val reviewedIds = reviewedMediaDao.getAllReviewedIds()
        return mediaStore.countMedia(query, reviewedIds)
    }

    override suspend fun markReviewed(mediaId: Long, decision: ReviewDecision, sizeBytes: Long) {
        reviewedMediaDao.upsert(
            ReviewedMediaEntity(
                mediaId = mediaId,
                decision = decision,
                reviewedAt = System.currentTimeMillis(),
                sizeBytes = sizeBytes,
            ),
        )
    }

    override suspend fun undoLastReview(mediaId: Long) {
        reviewedMediaDao.deleteById(mediaId)
    }

    override suspend fun recordSession(fotos: Int, bytes: Long) {
        sessionStatsDao.insert(
            SessionStatsEntity(
                fecha = System.currentTimeMillis(),
                fotosEliminadas = fotos,
                bytesLiberados = bytes,
            ),
        )
    }

    override fun observeTotalFreedBytes(): Flow<Long> = sessionStatsDao.observeTotalFreedBytes()

    override fun observeTotalDeletedCount(): Flow<Int> = sessionStatsDao.observeTotalDeletedCount()

    override suspend fun clearHistory() {
        reviewedMediaDao.clearAll()
        sessionStatsDao.clearAll()
    }

    override suspend fun pruneOrphanReviews() {
        val existingIds = mediaStore.getAllMediaIds()
        // Sin permiso de medios o galería vacía la lista llega vacía: no se toca el
        // historial para no borrarlo entero por un estado transitorio.
        if (existingIds.isEmpty()) return
        reviewedMediaDao.deleteOrphans(existingIds)
    }
}
