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
        /** `true` borra definitivo; `false` manda a la papelera del sistema. */
        val permanent: Boolean = false,
    ) : SummaryUiState

    /**
     * Borrado confirmado por el usuario.
     *
     * [permanent] decide el mensaje: solo el borrado definitivo libera espacio en
     * disco. `createTrashRequest` deja el archivo ocupando bytes hasta que el
     * sistema vacíe la papelera, así que ahí no se puede hablar de espacio liberado.
     */
    data class Success(
        val deletedBytes: Long,
        val deletedCount: Int,
        val permanent: Boolean = false,
    ) : SummaryUiState
}

/** Efectos de una sola vez que la pantalla debe atender. */
sealed interface SummaryEvent {

    /** Hay que lanzar el diálogo del sistema con esta petición. */
    data class LaunchDeletion(val request: DeletionRequest) : SummaryEvent

    /** Mostrar un snackbar con este texto. */
    data class ShowMessage(@param:StringRes val messageRes: Int) : SummaryEvent

    /**
     * [count] fotos de la ronda siguen en MediaStore después de confirmar: el
     * sistema no las borró. Se avisa en vez de dejarlo en un log.
     */
    data class ShowNotDeleted(val count: Int) : SummaryEvent
}
