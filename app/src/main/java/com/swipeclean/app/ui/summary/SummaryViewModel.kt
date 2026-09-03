package com.swipeclean.app.ui.summary

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
    ) { items, isDeleting, done ->
        when {
            done != null -> done
            items.isEmpty() -> SummaryUiState.Empty
            else -> SummaryUiState.Content(
                items = items,
                totalBytes = items.sumOf { it.sizeBytes },
                deleting = isDeleting,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState.Loading)

    private val _events = MutableSharedFlow<SummaryEvent>(extraBufferCapacity = 2)
    val events: SharedFlow<SummaryEvent> = _events.asSharedFlow()

    // Fotos de la ronda de borrado en curso: se usan al confirmar para registrar stats.
    private var pendingItems: List<MediaItem> = emptyList()

    /** Toque sobre una miniatura: la foto vuelve a conservarse y sale del grid. */
    fun onKeepInstead(item: MediaItem) {
        viewModelScope.launch {
            repository.markReviewed(item.id, ReviewDecision.KEPT, item.sizeBytes)
        }
    }

    fun onDeleteClicked() {
        val content = uiState.value as? SummaryUiState.Content ?: return
        if (content.deleting || content.items.isEmpty()) return
        deleting.value = true
        pendingItems = content.items
        viewModelScope.launch {
            try {
                val request = mediaDeleter.prepare(pendingItems.map { it.uri }, permanentDelete.value)
                _events.emit(SummaryEvent.LaunchDeletion(request))
            } catch (e: Exception) {
                deleting.value = false
                _events.emit(SummaryEvent.ShowMessage(R.string.delete_error))
            }
        }
    }

    fun onDeletionResult(result: DeletionResult) {
        when (result) {
            is DeletionResult.Confirmed -> onConfirmed()

            DeletionResult.Cancelled -> {
                // Vuelve al resumen intacto: no se marca ni se suma nada.
                deleting.value = false
                _events.tryEmit(SummaryEvent.ShowMessage(R.string.delete_cancelled))
            }

            is DeletionResult.Failed -> {
                deleting.value = false
                _events.tryEmit(SummaryEvent.ShowMessage(R.string.delete_error))
            }
        }
    }

    private fun onConfirmed() {
        viewModelScope.launch {
            // Se comprueba contra MediaStore qué fotos de la ronda ya no están: esas
            // son las eliminadas (o enviadas a la papelera). No se depende de lo que
            // devuelva el launcher, que se pierde si la pantalla rota con el diálogo.
            val stillPresent = repository.presentMediaIds(pendingItems.map { it.id })
            val deleted = pendingItems.filterNot { it.id in stillPresent }
            val bytes = deleted.sumOf { it.sizeBytes }
            val count = deleted.size

            if (count > 0) {
                repository.recordSession(count, bytes)
                repository.forgetReviewed(deleted.map { it.id })
            }
            deleting.value = false
            success.value = SummaryUiState.Success(freedBytes = bytes, freedCount = count)
        }
    }
}
