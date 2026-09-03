package com.swipeclean.app.domain.model

/**
 * Filtros de consulta a la galería. Todos son opcionales salvo [includeVideos].
 *
 * - [bucketId]: limita a un álbum concreto.
 * - [dateFromMillis] / [dateToMillis]: rango sobre la fecha de alta (inclusivo/exclusivo).
 * - [minSizeBytes]: descarta archivos por debajo de ese peso.
 * - [includeVideos]: si es `false`, solo se devuelven imágenes.
 */
data class MediaQuery(
    val bucketId: Long? = null,
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val minSizeBytes: Long = 0L,
    val includeVideos: Boolean = true,
) {
    companion object {
        /** Tamaño de página para la paginación real por limit/offset. */
        const val PAGE_SIZE = 100
    }
}
