package com.swipeclean.app.ui.swipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.domain.model.MediaQuery
import com.swipeclean.app.domain.model.ReviewDecision
import com.swipeclean.app.domain.repository.MediaRepository
import com.swipeclean.app.ui.navigation.Swipe
import com.swipeclean.app.ui.swipe.components.SwipeDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado y lógica de la pantalla de swipe.
 *
 * La aritmética de páginas, cursor y refill vive en [SwipeBuffer] (probada aparte).
 * Aquí quedan la orquestación, los contadores de sesión —que sobreviven a muerte
 * de proceso en [SavedStateHandle]— y el stack de deshacer, en memoria (basta para
 * rotación). Cada decisión se persiste en Room al momento vía
 * [MediaRepository.markReviewed] ([ReviewDecision.KEPT] queda firme;
 * [ReviewDecision.DELETED] es provisional hasta el resumen).
 */
@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: Swipe = savedStateHandle.toRoute<Swipe>()
    private val query: MediaQuery = args.toMediaQuery()

    private val buffer = SwipeBuffer<MediaItem>(
        pageSize = MediaQuery.PAGE_SIZE,
        idOf = MediaItem::id,
    ) { limit, exclude ->
        repository.getMediaPage(query, limit, exclude)
    }

    private var loadingInitial = true
    private var total = 0

    private data class UndoEntry(val item: MediaItem, val decision: ReviewDecision)

    private val undoStack = ArrayDeque<UndoEntry>()

    private var reviewedThisSession = savedStateHandle.get<Int>(KEY_REVIEWED) ?: 0
    private var markedCount = savedStateHandle.get<Int>(KEY_MARKED_COUNT) ?: 0
    private var markedBytes = savedStateHandle.get<Long>(KEY_MARKED_BYTES) ?: 0L

    private val _uiState = MutableStateFlow(SwipeUiState(screenTitle = args.titulo))
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            total = savedStateHandle.get<Int>(KEY_TOTAL)
                ?: repository.countMedia(query).also { savedStateHandle[KEY_TOTAL] = it }
            buffer.loadInitial()
            loadingInitial = false
            emitState()
        }
    }

    /** Deslizar (o botón): izquierda marca para eliminar, derecha conserva. */
    fun onDecision(direction: SwipeDirection) {
        val item = buffer.current() ?: return
        val decision =
            if (direction == SwipeDirection.LEFT) ReviewDecision.DELETED else ReviewDecision.KEPT

        viewModelScope.launch { repository.markReviewed(item.id, decision, item.sizeBytes) }

        undoStack.addLast(UndoEntry(item, decision))
        if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()

        if (decision == ReviewDecision.DELETED) {
            markedCount++
            markedBytes += item.sizeBytes
        }
        buffer.advance()
        reviewedThisSession++
        persistCounters()
        if (buffer.needsRefill) {
            viewModelScope.launch { if (buffer.refillIfNeeded()) emitState() }
        }
        emitState()
    }

    /** Deshace la última decisión de la sesión y devuelve la carta al tope. */
    fun onUndo() {
        val entry = undoStack.removeLastOrNull() ?: return

        viewModelScope.launch { repository.undoLastReview(entry.item.id) }

        if (entry.decision == ReviewDecision.DELETED) {
            markedCount--
            markedBytes -= entry.item.sizeBytes
        }
        buffer.retreat()
        reviewedThisSession--
        persistCounters()
        emitState()
    }

    private fun emitState() {
        val top = buffer.current()
        val finished = top == null && buffer.endReached && !loadingInitial
        _uiState.value = SwipeUiState(
            loading = loadingInitial || (top == null && !buffer.endReached && !finished),
            topCard = top,
            nextCards = buffer.ahead(SwipeBuffer.STACK_AHEAD),
            upcoming = buffer.ahead(SwipeBuffer.PRELOAD_AHEAD),
            currentIndex = (reviewedThisSession + 1).coerceAtMost(total.coerceAtLeast(1)),
            total = total,
            markedForDeleteCount = markedCount,
            markedForDeleteBytes = markedBytes,
            canUndo = undoStack.isNotEmpty(),
            finished = finished,
            screenTitle = args.titulo,
        )
    }

    private fun persistCounters() {
        savedStateHandle[KEY_REVIEWED] = reviewedThisSession
        savedStateHandle[KEY_MARKED_COUNT] = markedCount
        savedStateHandle[KEY_MARKED_BYTES] = markedBytes
    }

    private fun Swipe.toMediaQuery() = MediaQuery(
        bucketId = bucketId,
        dateFromMillis = dateFromMillis,
        dateToMillis = dateToMillis,
        minSizeBytes = minSizeBytes,
        mediaType = mediaType,
        screenshotsOnly = screenshotsOnly,
        sortOrder = sortOrder,
    )

    private companion object {
        const val UNDO_LIMIT = 10

        const val KEY_REVIEWED = "swipe_reviewed"
        const val KEY_MARKED_COUNT = "swipe_marked_count"
        const val KEY_MARKED_BYTES = "swipe_marked_bytes"
        const val KEY_TOTAL = "swipe_total"
    }
}
