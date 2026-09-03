package com.swipeclean.app.domain.model

/**
 * Decisión tomada sobre una foto durante una sesión de revisión.
 *
 * - [KEPT]: el usuario deslizó a la derecha, la conserva.
 * - [DELETED]: el usuario deslizó a la izquierda, queda marcada para borrado en lote.
 */
enum class ReviewDecision {
    KEPT,
    DELETED,
}
