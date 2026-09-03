package com.swipeclean.app.domain.model

/**
 * Agrupación de medios pendientes de revisar por mes calendario (según
 * `DATE_ADDED`), para la sección "Por fecha" de la Home.
 *
 * [startMillis]/[endMillis] delimitan el mes en la zona horaria local (inicio
 * inclusivo, fin exclusivo) y sirven como [MediaQuery.dateFromMillis] /
 * [MediaQuery.dateToMillis] al navegar al swipe de ese mes.
 */
data class MediaMonthGroup(
    val year: Int,
    val month: Int,
    val startMillis: Long,
    val endMillis: Long,
    val cantidad: Int,
    val pesoTotalBytes: Long,
)
