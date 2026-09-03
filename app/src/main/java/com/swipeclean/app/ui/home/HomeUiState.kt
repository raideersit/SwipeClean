package com.swipeclean.app.ui.home

import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.domain.model.MediaMonthGroup

/** Estado único de la pantalla Home, expuesto por [HomeViewModel]. */
sealed interface HomeUiState {

    /** Primera carga: todavía no se resolvió ni el permiso ni los datos. */
    data object Loading : HomeUiState

    /** Sin acceso a la galería. La navegación redirige a onboarding al verlo. */
    data object NoPermission : HomeUiState

    /** Hay permiso pero no queda nada por revisar. */
    data object Empty : HomeUiState

    data class Content(
        val freedBytesTotal: Long,
        val buckets: List<MediaBucket>,
        val monthGroups: List<MediaMonthGroup>,
        val partialAccess: Boolean = false,
    ) : HomeUiState
}
