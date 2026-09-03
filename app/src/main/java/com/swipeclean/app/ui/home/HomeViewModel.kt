package com.swipeclean.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipeclean.app.domain.model.MediaAccessLevel
import com.swipeclean.app.domain.permission.MediaPermissions
import com.swipeclean.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val permissionLevel = MutableStateFlow(MediaPermissions.accessLevel(context))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = permissionLevel
        .flatMapLatest { level ->
            if (level == MediaAccessLevel.NONE) {
                flowOf(HomeUiState.NoPermission)
            } else {
                combine(
                    repository.getBuckets(),
                    repository.getMonthGroups(),
                    repository.observeTotalFreedBytes(),
                ) { buckets, monthGroups, freedBytes ->
                    if (buckets.isEmpty() && monthGroups.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Content(
                            freedBytesTotal = freedBytes,
                            buckets = buckets,
                            monthGroups = monthGroups,
                            partialAccess = level == MediaAccessLevel.PARTIAL,
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Vuelve a leer el permiso del sistema; se llama al retomar la pantalla. */
    fun refreshPermission() {
        permissionLevel.value = MediaPermissions.accessLevel(context)
    }

    fun resetHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
