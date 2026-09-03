package com.swipeclean.app.ui.swipe

import com.swipeclean.app.domain.model.MediaItem

/**
 * Estado único de la pantalla de swipe, expuesto por [SwipeViewModel].
 *
 * [topCard] es la carta activa; [nextCards] son a lo sumo dos elementos para el
 * apilado visual. [currentIndex] y [total] alimentan el contador "12 / 214":
 * [total] se fija al abrir la sesión y no cambia aunque se revisen fotos.
 */
data class SwipeUiState(
    val loading: Boolean = true,
    val topCard: MediaItem? = null,
    val nextCards: List<MediaItem> = emptyList(),
    /** Próximos elementos (hasta 3) para precargar con Coil y evitar parpadeo. */
    val upcoming: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val total: Int = 0,
    val markedForDeleteCount: Int = 0,
    val markedForDeleteBytes: Long = 0L,
    val canUndo: Boolean = false,
    val finished: Boolean = false,
    val screenTitle: String = "",
)
