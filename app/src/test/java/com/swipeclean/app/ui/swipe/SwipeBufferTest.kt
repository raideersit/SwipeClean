package com.swipeclean.app.ui.swipe

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cubre la aritmética de [SwipeBuffer]: avanzar, deshacer, refill en el borde de
 * página y la invariante del punto 4 de la etapa 8 (ninguna foto se salta ni se
 * repite, aunque la exclusión que ve la fuente venga solo del buffer).
 */
class SwipeBufferTest {

    /**
     * Fuente de páginas sobre un universo fijo de ids. Solo excluye lo que se le
     * pasa: modela el peor caso del punto 4, en el que Room todavía no registró la
     * última decisión y lo único fiable es lo que el buffer ya tiene.
     */
    private class FakeSource(private val universe: List<Long>) : SwipeBuffer.PageSource<Long> {
        var fetches = 0
            private set
        val excludeSeen = mutableListOf<Set<Long>>()

        override suspend fun fetch(limit: Int, exclude: Set<Long>): List<Long> {
            fetches++
            excludeSeen += exclude.toSet()
            return universe.asSequence().filter { it !in exclude }.take(limit).toList()
        }
    }

    private fun newBuffer(
        universe: List<Long>,
        pageSize: Int,
    ): Pair<SwipeBuffer<Long>, FakeSource> {
        val source = FakeSource(universe)
        return SwipeBuffer(pageSize = pageSize, idOf = { it }, source = source) to source
    }

    @Test
    fun `loadInitial trae paginas hasta pasar el umbral de margen`() = runTest {
        // Umbral = REFILL_THRESHOLD + STACK_AHEAD = 22. Con páginas de 5 hacen falta
        // 5 fetches (25 > 22) para cortar.
        val (buffer, source) = newBuffer((1L..100L).toList(), pageSize = 5)

        buffer.loadInitial()

        assertEquals(5, source.fetches)
        assertEquals(25, buffer.size)
        assertEquals(1L, buffer.current())
        assertFalse(buffer.endReached)
    }

    @Test
    fun `avanzar mueve el actual y llena la ventana de adelante`() = runTest {
        val (buffer, _) = newBuffer((1L..50L).toList(), pageSize = 25)
        buffer.loadInitial()

        assertEquals(1L, buffer.current())
        assertEquals(listOf(2L, 3L), buffer.ahead(SwipeBuffer.STACK_AHEAD))

        assertTrue(buffer.advance())

        assertEquals(2L, buffer.current())
        assertEquals(1, buffer.cursor)
        assertEquals(listOf(3L, 4L, 5L), buffer.ahead(SwipeBuffer.PRELOAD_AHEAD))
    }

    @Test
    fun `ahead se recorta al llegar al final del buffer`() = runTest {
        val (buffer, _) = newBuffer((1L..3L).toList(), pageSize = 10)
        buffer.loadInitial()

        assertEquals(listOf(2L, 3L), buffer.ahead(SwipeBuffer.PRELOAD_AHEAD))
        buffer.advance()
        assertEquals(listOf(3L), buffer.ahead(SwipeBuffer.PRELOAD_AHEAD))
        buffer.advance()
        assertEquals(emptyList<Long>(), buffer.ahead(SwipeBuffer.PRELOAD_AHEAD))
    }

    @Test
    fun `deshacer devuelve el actual al elemento anterior`() = runTest {
        val (buffer, _) = newBuffer((1L..50L).toList(), pageSize = 25)
        buffer.loadInitial()
        buffer.advance()
        buffer.advance()
        assertEquals(3L, buffer.current())

        assertTrue(buffer.retreat())
        assertEquals(2L, buffer.current())

        assertTrue(buffer.retreat())
        assertEquals(1L, buffer.current())

        // Ya en el primero: no retrocede más.
        assertFalse(buffer.retreat())
        assertEquals(1L, buffer.current())
        assertEquals(0, buffer.cursor)
    }

    @Test
    fun `refill en el borde de pagina trae la siguiente excluyendo lo ya cargado`() = runTest {
        val (buffer, source) = newBuffer((1L..200L).toList(), pageSize = 25)
        buffer.loadInitial()
        assertEquals(1, source.fetches)
        assertEquals(25, buffer.size)

        // Avanza hasta dejar exactamente REFILL_THRESHOLD por delante.
        repeat(25 - SwipeBuffer.REFILL_THRESHOLD) { assertTrue(buffer.advance()) }
        assertEquals(SwipeBuffer.REFILL_THRESHOLD, buffer.remaining)
        assertTrue(buffer.needsRefill)

        assertTrue(buffer.refillIfNeeded())

        assertEquals(2, source.fetches)
        assertEquals(50, buffer.size)
        assertFalse(buffer.needsRefill)
        // La segunda página se pidió excluyendo TODO lo que ya estaba en el buffer.
        assertEquals((1L..25L).toSet(), source.excludeSeen[1])
    }

    @Test
    fun `pagina corta marca el final`() = runTest {
        val (buffer, source) = newBuffer((1L..12L).toList(), pageSize = 10)

        buffer.loadInitial()

        // 1ª página: 10 (llena). 2ª: 2 (< 10) -> endReached.
        assertEquals(2, source.fetches)
        assertEquals(12, buffer.size)
        assertTrue(buffer.endReached)
    }

    @Test
    fun `sin mas paginas no se vuelve a consultar la fuente`() = runTest {
        val (buffer, source) = newBuffer((1L..8L).toList(), pageSize = 10)
        buffer.loadInitial()
        assertTrue(buffer.endReached)
        val fetchesTrasCarga = source.fetches

        while (buffer.advance()) { /* drenar hasta el final */ }

        assertFalse(buffer.needsRefill)
        assertFalse(buffer.refillIfNeeded())
        assertEquals(fetchesTrasCarga, source.fetches)
        assertNull(buffer.current())
    }

    @Test
    fun `una corrida completa no salta ni repite ninguna foto`() = runTest {
        // Universo que no es múltiplo del tamaño de página, a propósito.
        val universe = (1L..73L).toList()
        val (buffer, _) = newBuffer(universe, pageSize = 10)
        buffer.loadInitial()

        val surfaced = mutableListOf<Long>()
        while (true) {
            val actual = buffer.current() ?: break
            surfaced += actual
            if (buffer.needsRefill) buffer.refillIfNeeded()
            buffer.advance()
        }

        // Igualdad de listas: orden, completitud y ausencia de repetidos de una vez.
        assertEquals(universe, surfaced)
    }

    @Test
    fun `deshacer despues de un refill mantiene la continuidad`() = runTest {
        val universe = (1L..60L).toList()
        val (buffer, _) = newBuffer(universe, pageSize = 25)
        buffer.loadInitial()

        repeat(24) { buffer.advance() }
        if (buffer.needsRefill) buffer.refillIfNeeded()
        // cursor en 24 -> actual es 25; el 26 ya vino en el refill.
        assertEquals(25L, buffer.current())
        buffer.advance()
        assertEquals(26L, buffer.current())

        buffer.retreat()
        assertEquals(25L, buffer.current())
        buffer.retreat()
        assertEquals(24L, buffer.current())
    }
}
