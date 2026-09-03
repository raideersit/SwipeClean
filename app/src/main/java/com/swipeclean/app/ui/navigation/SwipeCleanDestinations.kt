package com.swipeclean.app.ui.navigation

import com.swipeclean.app.domain.model.MediaSortOrder
import com.swipeclean.app.domain.model.MediaTypeFilter
import kotlinx.serialization.Serializable

/** Rutas de navegación type-safe. Ver [com.swipeclean.app.ui.navigation.SwipeCleanNavHost]. */
@Serializable
data object Onboarding

@Serializable
data object Home

/**
 * Filtro con el que se abre la pantalla de swipe, ya "aplanado" para viajar como
 * argumentos de navegación. [titulo] es el texto que muestra la top bar del swipe.
 */
@Serializable
data class Swipe(
    val bucketId: Long? = null,
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val minSizeBytes: Long = 0L,
    val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val screenshotsOnly: Boolean = false,
    val sortOrder: MediaSortOrder = MediaSortOrder.DATE_DESC,
    val titulo: String = "",
)

@Serializable
data object Summary

@Serializable
data object Settings
