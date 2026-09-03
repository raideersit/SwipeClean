package com.swipeclean.app.ui.settings

import com.swipeclean.app.domain.model.ThemeMode

/** Estado único de la pantalla de ajustes. */
data class SettingsUiState(
    val permanentDelete: Boolean = false,
    val includeVideos: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val totalFreedBytes: Long = 0L,
    val totalDeletedCount: Int = 0,
)
