package com.swipeclean.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipeclean.app.domain.model.ThemeMode
import com.swipeclean.app.domain.repository.MediaRepository
import com.swipeclean.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.preferences,
        mediaRepository.observeTotalFreedBytes(),
        mediaRepository.observeTotalDeletedCount(),
    ) { prefs, freedBytes, deletedCount ->
        SettingsUiState(
            permanentDelete = prefs.permanentDelete,
            includeVideos = prefs.includeVideos,
            themeMode = prefs.themeMode,
            totalFreedBytes = freedBytes,
            totalDeletedCount = deletedCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setPermanentDelete(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setPermanentDelete(enabled) }

    fun setIncludeVideos(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setIncludeVideos(enabled) }

    fun setThemeMode(mode: ThemeMode) =
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }

    fun resetHistory() =
        viewModelScope.launch { mediaRepository.clearHistory() }
}
