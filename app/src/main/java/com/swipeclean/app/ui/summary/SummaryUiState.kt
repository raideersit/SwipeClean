package com.swipeclean.app.ui.summary

import androidx.annotation.StringRes
import com.swipeclean.app.domain.deletion.DeletionRequest
import com.swipeclean.app.domain.model.MediaItem

/** Estado único de la pantalla de resumen. */
sealed interface SummaryUiState {

    data object Loading : SummaryUiState

    /** No quedó nada marcado para eliminar. */
    data object Empty : SummaryUiState

    data class Content(
        val items: List<MediaItem>,
        val totalBytes: Long,
        val deleting: Boolean = false,
    ) : SummaryUiState

    /** Borrado confirmado por el usuario. */
    data class Success(
        val freedBytes: Long,
        val freedCount: Int,
    ) : SummaryUiState
}

/** Efectos de una sola vez que la pantalla debe atender. */
sealed interface SummaryEvent {

    /** Hay que lanzar el diálogo del sistema con esta petición. */
    data class LaunchDeletion(val request: DeletionRequest) : SummaryEvent

    /** Mostrar un snackbar con este texto. */
    data class ShowMessage(@param:StringRes val messageRes: Int) : SummaryEvent
}
