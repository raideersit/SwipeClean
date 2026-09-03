package com.swipeclean.app.domain.repository

import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.domain.model.MediaQuery
import com.swipeclean.app.domain.model.ReviewDecision
import kotlinx.coroutines.flow.Flow

/**
 * Une la galería (MediaStore) con el historial local (Room).
 *
 * El filtrado de fotos ya revisadas es responsabilidad de esta capa y ocurre
 * siempre en la consulta, nunca recorriendo listas en memoria.
 */
interface MediaRepository {

    /** Álbumes de la galería con conteo y peso agregados. Se re-emite al cambiar MediaStore. */
    fun getBuckets(): Flow<List<MediaBucket>>

    /**
     * Página de elementos que cumplen [query], ordenada por fecha de alta descendente,
     * ya EXCLUYENDO los `mediaId` presentes en `reviewed_media`.
     */
    suspend fun getMediaPage(query: MediaQuery, offset: Int, limit: Int): List<MediaItem>

    /** Registra la decisión sobre una foto. Sobrescribe si el `mediaId` ya estaba. */
    suspend fun markReviewed(mediaId: Long, decision: ReviewDecision, sizeBytes: Long)

    /** Deshace la última decisión: borra el registro de `reviewed_media`. */
    suspend fun undoLastReview(mediaId: Long)

    /** Guarda las estadísticas de una sesión terminada. */
    suspend fun recordSession(fotos: Int, bytes: Long)

    /** Total histórico de bytes liberados (suma de todas las sesiones). */
    fun observeTotalFreedBytes(): Flow<Long>

    /** Reset de ajustes: borra todo el historial de revisiones y estadísticas. */
    suspend fun clearHistory()

    /**
     * Elimina registros de `reviewed_media` cuyos `mediaId` ya no existen en MediaStore
     * (fotos borradas desde fuera de la app). Se ejecuta al iniciar la app.
     */
    suspend fun pruneOrphanReviews()
}
