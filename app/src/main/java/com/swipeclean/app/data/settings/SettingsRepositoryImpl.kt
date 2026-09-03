package com.swipeclean.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swipeclean.app.domain.model.ThemeMode
import com.swipeclean.app.domain.model.UserPreferences
import com.swipeclean.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Único DataStore de preferencias de la app. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private val dataStore: DataStore<Preferences> get() = context.settingsDataStore

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            permanentDelete = prefs[KEY_PERMANENT_DELETE] ?: false,
            includeVideos = prefs[KEY_INCLUDE_VIDEOS] ?: true,
            themeMode = prefs[KEY_THEME_MODE]?.let(::themeModeOf) ?: ThemeMode.SYSTEM,
        )
    }

    override suspend fun setPermanentDelete(enabled: Boolean) {
        dataStore.edit { it[KEY_PERMANENT_DELETE] = enabled }
    }

    override suspend fun setIncludeVideos(enabled: Boolean) {
        dataStore.edit { it[KEY_INCLUDE_VIDEOS] = enabled }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private fun themeModeOf(raw: String): ThemeMode =
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)

    private companion object {
        val KEY_PERMANENT_DELETE = booleanPreferencesKey("permanent_delete")
        val KEY_INCLUDE_VIDEOS = booleanPreferencesKey("include_videos")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
