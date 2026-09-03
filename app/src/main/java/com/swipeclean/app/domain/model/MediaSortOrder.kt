package com.swipeclean.app.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Orden de una [MediaQuery]. Ambos son siempre descendentes: lo más nuevo o lo
 * más pesado primero.
 *
 * [Keep]: su serializer no debe ofuscarse si algún día se activa R8.
 */
@Keep
@Serializable
enum class MediaSortOrder {
    DATE_DESC,
    SIZE_DESC,
}
