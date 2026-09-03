package com.swipeclean.app.domain.repository

import com.swipeclean.app.domain.model.ThemeMode
import com.swipeclean.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Lee y escribe los ajustes de la app. Respaldado por DataStore. */
interface SettingsRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setPermanentDelete(enabled: Boolean)

    suspend fun setIncludeVideos(enabled: Boolean)

    suspend fun setThemeMode(mode: ThemeMode)
}
