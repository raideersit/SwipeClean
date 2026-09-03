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
 * Carga por páginas ([MediaQuery.PAGE_SIZE]) sobre un buffer que no se vacía: al
 * revisar solo avanza [cursor], así deshacer es inmediato. Cada decisión se
 * persiste en Room al momento vía [MediaRepository.markReviewed] ([ReviewDecision.KEPT]
 * queda firme; [ReviewDecision.DELETED] es provisional hasta el resumen).
 *
 * Los contadores de la sesión sobreviven a muerte de proceso en [SavedStateHandle];
 * el stack de deshacer vive en memoria (basta para rotación).
 */
@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args: Swipe = savedStateHandle.toRoute<Swipe>()
    private val query: MediaQuery = args.toMediaQuery()

    private val buffer = mutableListOf<MediaItem>()
    private var cursor = 0
    private var endReached = false
    private var refilling = false
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
            fillInitial()
            loadingInitial = false
            emitState()
        }
    }

    /** Deslizar (o botón): izquierda marca para eliminar, derecha conserva. */
    fun onDecision(direction: SwipeDirection) {
        val item = buffer.getOrNull(cursor) ?: return
        val decision =
            if (direction == SwipeDirection.LEFT) ReviewDecision.DELETED else ReviewDecision.KEPT

        viewModelScope.launch { repository.markReviewed(item.id, decision, item.sizeBytes) }

        undoStack.addLast(UndoEntry(item, decision))
        if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()

        if (decision == ReviewDecision.DELETED) {
            markedCount++
            markedBytes += item.sizeBytes
        }
        cursor++
        reviewedThisSession++
        persistCounters()
        maybeRefill()
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
        cursor--
        reviewedThisSession--
        persistCounters()
        emitState()
    }

    private suspend fun fillInitial() {
        while (!endReached && buffer.size - cursor <= PAGE_REFILL_THRESHOLD + STACK_AHEAD) {
            fetchNextPage()
        }
    }

    private fun maybeRefill() {
        if (endReached || refilling) return
        if (buffer.size - cursor > PAGE_REFILL_THRESHOLD) return
        refilling = true
        viewModelScope.launch {
            fetchNextPage()
            refilling = false
            emitState()
        }
    }

    /**
     * Trae la siguiente página. El offset se calcula como `buffer.size - cursor`
     * porque los elementos ya decididos (índices `0 until cursor`) están en Room y
     * la consulta los excluye: sin ese ajuste la paginación se saltaría fotos.
     */
    private suspend fun fetchNextPage() {
        val offset = (buffer.size - cursor).coerceAtLeast(0)
        val page = repository.getMediaPage(query, offset, MediaQuery.PAGE_SIZE)
        val known = buffer.mapTo(HashSet(buffer.size)) { it.id }
        buffer.addAll(page.filter { it.id !in known })
        if (page.size < MediaQuery.PAGE_SIZE) endReached = true
    }

    private fun emitState() {
        val top = buffer.getOrNull(cursor)
        val nextFrom = (cursor + 1).coerceAtMost(buffer.size)
        val stackTo = (cursor + 1 + STACK_AHEAD).coerceAtMost(buffer.size)
        val preloadTo = (cursor + 1 + PRELOAD_AHEAD).coerceAtMost(buffer.size)
        val finished = top == null && endReached && !loadingInitial
        _uiState.value = SwipeUiState(
            loading = loadingInitial || (top == null && !endReached && !finished),
            topCard = top,
            nextCards = buffer.subList(nextFrom, stackTo).toList(),
            upcoming = buffer.subList(nextFrom, preloadTo).toList(),
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
        const val PAGE_REFILL_THRESHOLD = 20
        const val STACK_AHEAD = 2
        const val PRELOAD_AHEAD = 3
        const val UNDO_LIMIT = 10

        const val KEY_REVIEWED = "swipe_reviewed"
        const val KEY_MARKED_COUNT = "swipe_marked_count"
        const val KEY_MARKED_BYTES = "swipe_marked_bytes"
        const val KEY_TOTAL = "swipe_total"
    }
}
