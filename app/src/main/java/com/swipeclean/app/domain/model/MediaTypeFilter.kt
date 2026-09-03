package com.swipeclean.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Qué tipo de medio incluir en una [MediaQuery]. Se serializa como argumento de
 * navegación al filtro de la pantalla de swipe.
 */
@Serializable
enum class MediaTypeFilter {
    ALL,
    IMAGES,
    VIDEOS,
}
