package com.swipeclean.app.domain.model

/** Cómo debe resolverse el tema de la app. */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * Ajustes editables por el usuario, persistidos con DataStore.
 *
 * - [permanentDelete]: `true` borra de forma definitiva (`createDeleteRequest`);
 *   `false` (por defecto) mueve a la papelera del sistema (`createTrashRequest`).
 * - [includeVideos]: incluir videos además de fotos en la galería.
 * - [themeMode]: claro, oscuro o el del sistema.
 */
data class UserPreferences(
    val permanentDelete: Boolean = false,
    val includeVideos: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
