package com.swipeclean.app.ui.swipe

/**
 * Aritmética de paginación de la pantalla de swipe, aislada de [SwipeViewModel]
 * para poder probarla sin Android. Es genérica en [T] justamente por eso: en los
 * tests el elemento es un id pelado; en producción es un `MediaItem`.
 *
 * El buffer es append-only: revisar una foto solo avanza [cursor], así deshacer es
 * inmediato. Cada página se pide desde el principio excluyendo lo que ya está en el
 * buffer ([PageSource]); no hay offset. Ese es el punto: la exclusión sale del
 * propio buffer, no de Room, así que ninguna foto se salta ni se repite aunque la
 * escritura del ViewModel en Room llegue tarde.
 *
 * No lanza corrutinas ni toca dispatchers. [loadInitial] y [refillIfNeeded] son
 * `suspend` y el llamante las agenda. Deben invocarse desde un contexto de un solo
 * hilo (`viewModelScope`): de ahí depende la guarda de reentrada.
 */
internal class SwipeBuffer<T>(
    private val pageSize: Int,
    private val idOf: (T) -> Long,
    private val source: PageSource<T>,
) {

    /** Trae hasta [limit] elementos que no estén en [exclude]. */
    fun interface PageSource<T> {
        suspend fun fetch(limit: Int, exclude: Set<Long>): List<T>
    }

    private val items = mutableListOf<T>()
    private val ids = HashSet<Long>()

    var cursor = 0
        private set
    var endReached = false
        private set
    private var busy = false

    val size: Int get() = items.size

    /** Elementos sin revisar por delante del cursor, el actual incluido. */
    val remaining: Int get() = items.size - cursor

    /** `true` si conviene traer otra página ya. Barato: no suspende. */
    val needsRefill: Boolean
        get() = !endReached && !busy && remaining <= REFILL_THRESHOLD

    fun current(): T? = items.getOrNull(cursor)

    /**
     * Hasta [count] elementos a partir del siguiente al actual, recortado al final
     * del buffer. Alimenta el apilado visual y la precarga de Coil.
     */
    fun ahead(count: Int): List<T> {
        val from = (cursor + 1).coerceIn(0, items.size)
        val to = (from + count).coerceAtMost(items.size)
        return items.subList(from, to).toList()
    }

    /** Avanza al siguiente. `false` si ya no hay actual que dejar atrás. */
    fun advance(): Boolean {
        if (cursor >= items.size) return false
        cursor++
        return true
    }

    /** Retrocede uno (deshacer). `false` si ya está en el primero. */
    fun retreat(): Boolean {
        if (cursor <= 0) return false
        cursor--
        return true
    }

    /** Llena el buffer hasta tener margen por delante del cursor, o hasta el final. */
    suspend fun loadInitial() {
        if (busy) return
        busy = true
        try {
            while (!endReached && remaining <= REFILL_THRESHOLD + STACK_AHEAD) {
                fetchNextPage()
            }
        } finally {
            busy = false
        }
    }

    /**
     * Trae una página más si el margen bajó del umbral. Reentrante-seguro por
     * [busy], que se fija antes de la primera suspensión.
     *
     * @return `true` si trajo algo, para que el llamante re-emita estado.
     */
    suspend fun refillIfNeeded(): Boolean {
        if (endReached || busy || remaining > REFILL_THRESHOLD) return false
        busy = true
        try {
            fetchNextPage()
        } finally {
            busy = false
        }
        return true
    }

    private suspend fun fetchNextPage() {
        val page = source.fetch(pageSize, ids)
        var added = 0
        for (item in page) {
            if (ids.add(idOf(item))) {
                items.add(item)
                added++
            }
        }
        // Página incompleta -> no hay más. Página llena sin nada nuevo -> la fuente
        // no está excluyendo bien; se corta igual para no girar en falso.
        if (page.size < pageSize || added == 0) endReached = true
    }

    companion object {
        /** Si quedan estas o menos por delante del cursor, se pide otra página. */
        const val REFILL_THRESHOLD = 20

        /** Cartas visibles apiladas detrás de la activa. */
        const val STACK_AHEAD = 2

        /** Elementos a precargar con Coil por delante de la activa. */
        const val PRELOAD_AHEAD = 3
    }
}
