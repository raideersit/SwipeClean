package com.swipeclean.app.util

import java.util.Locale
import kotlin.math.abs

private val UNIDADES = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
private const val BASE = 1024.0
private val LOCALE_ES = Locale.forLanguageTag("es")

/**
 * Convierte una cantidad de bytes a texto legible en español: "340 MB", "1,2 GB".
 *
 * Base 1024. Los bytes se muestran sin decimales; a partir de KB se usa un decimal
 * salvo que el valor sea entero. El separador decimal es la coma (locale es).
 */
fun formatBytes(bytes: Long): String {
    if (abs(bytes) < BASE) return "$bytes B"

    var valor = bytes.toDouble()
    var indice = 0
    while (abs(valor) >= BASE && indice < UNIDADES.lastIndex) {
        valor /= BASE
        indice++
    }

    // Sin decimal si el redondeo a un decimal da un entero exacto (p. ej. "2 GB").
    val redondeado = Math.round(valor * 10.0) / 10.0
    val patron = if (redondeado % 1.0 == 0.0) "%.0f %s" else "%.1f %s"
    return String.format(LOCALE_ES, patron, redondeado, UNIDADES[indice])
}
