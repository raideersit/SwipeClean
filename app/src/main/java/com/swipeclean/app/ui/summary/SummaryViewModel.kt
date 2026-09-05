package com.swipeclean.app.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipeclean.app.R
import com.swipeclean.app.domain.deletion.DeletionResult
import com.swipeclean.app.domain.deletion.MediaDeleter
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.domain.model.ReviewDecision
import com.swipeclean.app.domain.repository.MediaRepository
import com.swipeclean.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val mediaDeleter: MediaDeleter,
    private val savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val permanentDelete: StateFlow<Boolean> = settingsRepository.preferences
        .map { it.permanentDelete }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val deleting = MutableStateFlow(false)
    private val success = MutableStateFlow<SummaryUiState.Success?>(null)

    val uiState: StateFlow<SummaryUiState> = combine(
        repository.observeMarkedForDeletion(),
        deleting,
        success,
        permanentDelete,
    ) { items, isDeleting, done, permanent ->
        when {
            done != null -> done
            items.isEmpty() -> SummaryUiState.Empty
            else -> SummaryUiState.Content(
                items = items,
                totalBytes = items.sumOf { it.sizeBytes },
                deleting = isDeleting,
                permanent = permanent,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState.Loading)

    private val _events = MutableSharedFlow<SummaryEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<SummaryEvent> = _events.asSharedFlow()

    /** Lo mínimo que hay que recordar de una foto para reconciliar la ronda. */
    private data class PendingItem(val id: Long, val sizeBytes: Long)

    /**
     * Fotos de la ronda de borrado en curso.
     *
     * Vive en [SavedStateHandle] y no en un campo del ViewModel porque el diálogo
     * de borrado es otra Activity: si el sistema mata el proceso con el diálogo
     * arriba, un campo normal vuelve vacío y la ronda se pierde entera (ni se
     * registra la sesión ni se limpia el historial). Solo hacen falta id y tamaño,
     * así que se guardan como dos `LongArray` en vez de volver `MediaItem` parcelable.
     */
    private var pending: List<PendingItem>
        get() {
            val ids = savedStateHandle.get<LongArray>(KEY_PENDING_IDS) ?: return emptyList()
            val sizes = savedStateHandle.get<LongArray>(KEY_PENDING_SIZES) ?: return emptyList()
            if (ids.size != sizes.size) return emptyList()
            return ids.zip(sizes) { id, size -> PendingItem(id, size) }
        }
        set(value) {
            savedStateHandle[KEY_PENDING_IDS] = value.map { it.id }.toLongArray()
            savedStateHandle[KEY_PENDING_SIZES] = value.map { it.sizeBytes }.toLongArray()
        }

    init {
        // Quedó una ronda a medias: el proceso murió mientras el diálogo del sistema
        // estaba arriba. Se reconcilia contra MediaStore, que sabe qué se borró de
        // verdad aunque el resultado del launcher se haya perdido.
        if (pending.isNotEmpty()) reconcile(restored = true)
    }

    /** Toque sobre una miniatura: la foto vuelve a conservarse y sale del grid. */
    fun onKeepInstead(item: MediaItem) {
        viewModelScope.launch {
            repository.markReviewed(item.id, ReviewDecision.KEPT, item.sizeBytes)
        }
    }

    fun onDeleteClicked() {
        val content = uiState.value as? SummaryUiState.Content ?: return
        if (content.deleting || content.items.isEmpty()) return

        val permanent = permanentDelete.value
        deleting.value = true
        pending = content.items.map { PendingItem(it.id, it.sizeBytes) }
        savedStateHandle[KEY_PENDING_PERMANENT] = permanent

        viewModelScope.launch {
            try {
                val request = mediaDeleter.prepare(content.items.map { it.uri }, permanent)
                _events.emit(SummaryEvent.LaunchDeletion(request))
            } catch (e: Exception) {
                clearPending()
                deleting.value = false
                _events.emit(SummaryEvent.ShowMessage(R.string.delete_error))
            }
        }
    }

    fun onDeletionResult(result: DeletionResult) {
        when (result) {
            is DeletionResult.Confirmed -> reconcile(restored = false)

            DeletionResult.Cancelled -> {
                // Vuelve al resumen intacto: no se marca ni se suma nada.
                clearPending()
                deleting.value = false
                _events.tryEmit(SummaryEvent.ShowMessage(R.string.delete_cancelled))
            }

            is DeletionResult.Failed -> {
                clearPending()
                deleting.value = false
                _events.tryEmit(SummaryEvent.ShowMessage(R.string.delete_error))
            }
        }
    }

    /**
     * Cierra la ronda comparando lo pendiente contra MediaStore.
     *
     * MediaStore es la única fuente fiable: lo que devuelve el launcher se pierde si
     * la pantalla rota o el proceso muere con el diálogo arriba. Por eso tampoco
     * importa que una ruta de borrado no informe qué alcanzó a borrar antes de
     * interrumpirse — lo que cuenta es qué desapareció, y eso se mide aquí.
     *
     * Con [restored] `true` no se sabe si el usuario llegó a confirmar, así que si
     * no desapareció nada se calla en vez de acusar un fallo que quizá fue un
     * cancelar.
     */
    private fun reconcile(restored: Boolean) {
        val items = pending
        if (items.isEmpty()) {
            deleting.value = false
            return
        }
        val permanent = savedStateHandle.get<Boolean>(KEY_PENDING_PERMANENT) ?: false

        viewModelScope.launch {
            val stillPresent = repository.presentMediaIds(items.map { it.id })
            val deleted = items.filterNot { it.id in stillPresent }
            val bytes = deleted.sumOf { it.sizeBytes }
            val count = deleted.size

            if (count > 0) {
                repository.recordSession(count, bytes)
                repository.forgetReviewed(deleted.map { it.id })
            }
            val notDeleted = items.size - count
            clearPending()
            deleting.value = false

            when {
                count > 0 -> {
                    success.value = SummaryUiState.Success(bytes, count, permanent)
                    if (notDeleted > 0) _events.emit(SummaryEvent.ShowNotDeleted(notDeleted))
                }

                !restored -> _events.emit(SummaryEvent.ShowNotDeleted(notDeleted))
            }
        }
    }

    private fun clearPending() {
        savedStateHandle.remove<LongArray>(KEY_PENDING_IDS)
        savedStateHandle.remove<LongArray>(KEY_PENDING_SIZES)
        savedStateHandle.remove<Boolean>(KEY_PENDING_PERMANENT)
    }

    private companion object {
        const val KEY_PENDING_IDS = "summary_pending_ids"
        const val KEY_PENDING_SIZES = "summary_pending_sizes"
        const val KEY_PENDING_PERMANENT = "summary_pending_permanent"
    }
}
