package com.swipeclean.app.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.domain.model.MediaMonthGroup
import com.swipeclean.app.ui.components.PartialAccessBanner
import com.swipeclean.app.ui.home.components.BucketCard
import com.swipeclean.app.ui.home.components.HomeEmptyState
import com.swipeclean.app.ui.home.components.HomeLoadingPlaceholder
import com.swipeclean.app.ui.home.components.MonthGroupRow
import com.swipeclean.app.ui.home.components.monthGroupLabel
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import com.swipeclean.app.util.formatBytes

/**
 * Pantalla Home. Puramente de presentación: todo el estado llega en [uiState] y
 * las acciones del usuario se emiten como lambdas, sin lógica propia salvo el
 * armado visual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefreshPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBucket: (MediaBucket) -> Unit,
    onOpenChip: (HomeChip) -> Unit,
    onOpenMonth: (MediaMonthGroup, titulo: String) -> Unit,
    onResetHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onExpandSelection: () -> Unit = {},
) {
    LifecycleAwarePermissionRefresh(onRefreshPermission)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.home_settings_cd),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is HomeUiState.Loading, HomeUiState.NoPermission ->
                HomeLoadingPlaceholder(modifier = Modifier.padding(innerPadding))

            HomeUiState.Empty ->
                HomeEmptyState(onResetHistory = onResetHistory, modifier = Modifier.padding(innerPadding))

            is HomeUiState.Content -> HomeContent(
                state = uiState,
                onOpenChip = onOpenChip,
                onOpenBucket = onOpenBucket,
                onOpenMonth = onOpenMonth,
                onExpandSelection = onExpandSelection,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onOpenChip: (HomeChip) -> Unit,
    onOpenBucket: (MediaBucket) -> Unit,
    onOpenMonth: (MediaMonthGroup, titulo: String) -> Unit,
    onExpandSelection: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.partialAccess) {
            item { PartialAccessBanner(onExpandSelection = onExpandSelection) }
        }

        item {
            Text(
                text = stringResource(R.string.home_freed_space, formatBytes(state.freedBytesTotal)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { onOpenChip(HomeChip.SCREENSHOTS) },
                    label = { Text(stringResource(R.string.home_chip_screenshots)) },
                )
                AssistChip(
                    onClick = { onOpenChip(HomeChip.HEAVIEST_PHOTOS) },
                    label = { Text(stringResource(R.string.home_chip_heaviest)) },
                )
                AssistChip(
                    onClick = { onOpenChip(HomeChip.VIDEOS) },
                    label = { Text(stringResource(R.string.home_chip_videos)) },
                )
            }
        }

        if (state.buckets.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_section_folders),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(state.buckets.chunked(2)) { fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    fila.forEach { bucket ->
                        BucketCard(
                            bucket = bucket,
                            onClick = { onOpenBucket(bucket) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (fila.size == 1) {
                        Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }

        if (state.monthGroups.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_section_by_date),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(state.monthGroups) { grupo ->
                MonthGroupRow(
                    group = grupo,
                    onClick = { onOpenMonth(grupo, monthGroupLabel(grupo)) },
                )
            }
        }
    }
}

@Composable
private fun LifecycleAwarePermissionRefresh(onRefreshPermission: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefreshPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Preview(showBackground = true, name = "Cargando")
@Composable
private fun HomeScreenLoadingPreview() {
    SwipeCleanTheme {
        HomeScreen(
            uiState = HomeUiState.Loading,
            onRefreshPermission = {},
            onOpenSettings = {},
            onOpenBucket = {},
            onOpenChip = {},
            onOpenMonth = { _, _ -> },
            onResetHistory = {},
        )
    }
}

@Preview(showBackground = true, name = "Vacío")
@Composable
private fun HomeScreenEmptyPreview() {
    SwipeCleanTheme {
        HomeScreen(
            uiState = HomeUiState.Empty,
            onRefreshPermission = {},
            onOpenSettings = {},
            onOpenBucket = {},
            onOpenChip = {},
            onOpenMonth = { _, _ -> },
            onResetHistory = {},
        )
    }
}

@Preview(showBackground = true, name = "Con contenido")
@Composable
private fun HomeScreenContentPreview() {
    SwipeCleanTheme {
        HomeScreen(
            uiState = HomeUiState.Content(
                freedBytesTotal = 1_400L * 1024 * 1024,
                buckets = listOf(
                    MediaBucket(1L, "Cámara", 128, 340L * 1024 * 1024, null),
                    MediaBucket(2L, "WhatsApp", 64, 120L * 1024 * 1024, null),
                    MediaBucket(3L, "Descargas", 12, 45L * 1024 * 1024, null),
                ),
                monthGroups = listOf(
                    MediaMonthGroup(2026, 9, 0L, 0L, 42, 900L * 1024 * 1024),
                    MediaMonthGroup(2026, 8, 0L, 0L, 87, 1_600L * 1024 * 1024),
                ),
                partialAccess = false,
            ),
            onRefreshPermission = {},
            onOpenSettings = {},
            onOpenBucket = {},
            onOpenChip = {},
            onOpenMonth = { _, _ -> },
            onResetHistory = {},
        )
    }
}

@Preview(showBackground = true, name = "Acceso parcial")
@Composable
private fun HomeScreenPartialAccessPreview() {
    SwipeCleanTheme {
        HomeScreen(
            uiState = HomeUiState.Content(
                freedBytesTotal = 0L,
                buckets = listOf(MediaBucket(1L, "Seleccionadas", 8, 20L * 1024 * 1024, null)),
                monthGroups = emptyList(),
                partialAccess = true,
            ),
            onRefreshPermission = {},
            onOpenSettings = {},
            onOpenBucket = {},
            onOpenChip = {},
            onOpenMonth = { _, _ -> },
            onResetHistory = {},
        )
    }
}
