package com.swipeclean.app.domain.repository

import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.domain.model.MediaMonthGroup
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

    /**
     * Álbumes con fotos pendientes de revisar, con conteo y peso agregados.
     * Se re-emite al cambiar MediaStore. [query] permite acotar (p. ej. excluir videos).
     */
    fun getBuckets(query: MediaQuery = MediaQuery()): Flow<List<MediaBucket>>

    /**
     * Meses con fotos pendientes de revisar, con conteo y peso agregados.
     * Se re-emite al cambiar MediaStore. [query] permite acotar (p. ej. excluir videos).
     */
    fun getMonthGroups(query: MediaQuery = MediaQuery()): Flow<List<MediaMonthGroup>>

    /**
     * Fotos actualmente marcadas para eliminar ([ReviewDecision.DELETED]), resueltas
     * a [MediaItem]. Alimenta el grid del resumen; se re-emite al cambiar el historial.
     */
    fun observeMarkedForDeletion(): Flow<List<MediaItem>>

    /**
     * De [mediaIds], cuáles siguen existiendo en MediaStore. Tras confirmar el
     * borrado, los que faltan son los que realmente se eliminaron (o fueron a la
     * papelera), sin depender de lo que devuelva el diálogo del sistema.
     */
    suspend fun presentMediaIds(mediaIds: List<Long>): Set<Long>

    /**
     * Olvida estos `mediaId` del historial de revisiones. Se usa tras confirmar el
     * borrado en lote: la foto ya no existe, su registro deja de tener sentido.
     */
    suspend fun forgetReviewed(mediaIds: List<Long>)

    /**
     * Página de elementos que cumplen [query], ordenada por fecha de alta descendente,
     * ya EXCLUYENDO los `mediaId` presentes en `reviewed_media`.
     */
    suspend fun getMediaPage(query: MediaQuery, offset: Int, limit: Int): List<MediaItem>

    /**
     * Cantidad total de elementos que cumplen [query] y aún no están en
     * `reviewed_media`. Se calcula una vez al abrir la sesión de swipe.
     */
    suspend fun countMedia(query: MediaQuery): Int

    /** Registra la decisión sobre una foto. Sobrescribe si el `mediaId` ya estaba. */
    suspend fun markReviewed(mediaId: Long, decision: ReviewDecision, sizeBytes: Long)

    /** Deshace la última decisión: borra el registro de `reviewed_media`. */
    suspend fun undoLastReview(mediaId: Long)

    /** Guarda las estadísticas de una sesión terminada. */
    suspend fun recordSession(fotos: Int, bytes: Long)

    /** Total histórico de bytes liberados (suma de todas las sesiones). */
    fun observeTotalFreedBytes(): Flow<Long>

    /** Total histórico de fotos eliminadas (suma de todas las sesiones). */
    fun observeTotalDeletedCount(): Flow<Int>

    /** Reset de ajustes: borra todo el historial de revisiones y estadísticas. */
    suspend fun clearHistory()

    /**
     * Elimina registros de `reviewed_media` cuyos `mediaId` ya no existen en MediaStore
     * (fotos borradas desde fuera de la app). Se ejecuta al iniciar la app.
     */
    suspend fun pruneOrphanReviews()
}
