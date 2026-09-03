package com.swipeclean.app.domain.model

/**
 * Filtros de consulta a la galería. Todos son opcionales salvo [mediaType].
 *
 * - [bucketId]: limita a un álbum concreto.
 * - [dateFromMillis] / [dateToMillis]: rango sobre la fecha de alta (inclusivo/exclusivo).
 * - [minSizeBytes]: descarta archivos por debajo de ese peso.
 * - [mediaType]: qué tipo de medio incluir (todos, solo imágenes o solo videos).
 * - [screenshotsOnly]: si es `true`, solo capturas de pantalla.
 * - [sortOrder]: orden de la consulta, siempre descendente.
 */
data class MediaQuery(
    val bucketId: Long? = null,
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val minSizeBytes: Long = 0L,
    val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val screenshotsOnly: Boolean = false,
    val sortOrder: MediaSortOrder = MediaSortOrder.DATE_DESC,
) {
    companion object {
        /** Tamaño de página para la paginación real por limit/offset. */
        const val PAGE_SIZE = 100
    }
}
