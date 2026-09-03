package com.swipeclean.app.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Qué tipo de medio incluir en una [MediaQuery]. Se serializa como argumento de
 * navegación al filtro de la pantalla de swipe.
 *
 * [Keep]: su serializer no debe ofuscarse si algún día se activa R8.
 */
@Keep
@Serializable
enum class MediaTypeFilter {
    ALL,
    IMAGES,
    VIDEOS,
}
