package com.swipeclean.app.ui.summary

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.ui.media.rememberDeletionLauncher
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import com.swipeclean.app.util.formatBytes

/**
 * Resumen de fin de sesión: grid de lo marcado para eliminar, borrado en lote y
 * pantalla de éxito. Todo el estado llega de [SummaryViewModel]; esta capa solo
 * traduce eventos ([SummaryEvent]) a diálogo del sistema y snackbars.
 */
@Composable
fun SummaryScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalPlatformContext.current

    val deletionLauncher = rememberDeletionLauncher(onResult = viewModel::onDeletionResult)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SummaryEvent.LaunchDeletion -> deletionLauncher.launch(event.request)
                is SummaryEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))

                is SummaryEvent.ShowNotDeleted -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(
                        R.plurals.summary_not_deleted,
                        event.count,
                        event.count,
                    ),
                )
            }
        }
    }

    SummaryScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onFinish = onFinish,
        onKeepInstead = viewModel::onKeepInstead,
        onDelete = viewModel::onDeleteClicked,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreenContent(
    state: SummaryUiState,
    snackbarHostState: SnackbarHostState,
    onFinish: () -> Unit,
    onKeepInstead: (MediaItem) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is SummaryUiState.Success) {
        SuccessContent(state = state, onFinish = onFinish, modifier = modifier)
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title)) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.summary_back_cd),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (state) {
            SummaryUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            SummaryUiState.Empty -> EmptyContent(
                onFinish = onFinish,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is SummaryUiState.Content -> GridContent(
                state = state,
                contentPadding = innerPadding,
                onKeepInstead = onKeepInstead,
                onDelete = onDelete,
            )

            is SummaryUiState.Success -> Unit // atendido arriba
        }
    }
}

@Composable
private fun GridContent(
    state: SummaryUiState.Content,
    contentPadding: PaddingValues,
    onKeepInstead: (MediaItem) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        Text(
            text = stringResource(R.string.summary_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                DeletionThumbnail(item = item, onClick = { onKeepInstead(item) })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = pluralStringResource(
                    // Solo el borrado definitivo libera espacio: con la papelera el
                    // archivo se queda en disco hasta que el sistema la vacíe.
                    if (state.permanent) R.plurals.summary_will_free else R.plurals.summary_will_trash,
                    state.items.size,
                    formatBytes(state.totalBytes),
                    state.items.size,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onDelete,
                enabled = !state.deleting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.deleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.summary_deleting))
                } else {
                    Text(stringResource(R.string.summary_delete))
                }
            }
        }
    }
}

@Composable
private fun DeletionThumbnail(item: MediaItem, onClick: () -> Unit) {
    val errorPainter = painterResource(R.drawable.ic_media_error)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.summary_thumb_remove_cd, item.displayName),
            contentScale = ContentScale.Crop,
            error = errorPainter,
            fallback = errorPainter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
        )
    }
}

@Composable
private fun EmptyContent(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.summary_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.summary_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onFinish) {
            Text(stringResource(R.string.summary_go_home))
        }
    }
}

@Composable
private fun SuccessContent(
    state: SummaryUiState.Success,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.summary_success_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (state.permanent) R.string.summary_success_freed else R.string.summary_success_trashed,
                formatBytes(state.deletedBytes),
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = pluralStringResource(
                R.plurals.summary_success_detail,
                state.deletedCount,
                state.deletedCount,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!state.permanent) {
            // Aclara que los bytes siguen ocupados: la papelera no libera espacio.
            Text(
                text = stringResource(R.string.summary_success_trash_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.summary_keep_cleaning))
        }
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.summary_go_home))
        }
    }
}

private fun previewItem(id: Long) = MediaItem(
    id = id,
    uri = Uri.EMPTY,
    displayName = "IMG_$id.jpg",
    sizeBytes = 4_000_000L + id * 1000,
    dateAddedMillis = 0L,
    bucketId = 1L,
    bucketName = "Cámara",
    mimeType = "image/jpeg",
    isVideo = false,
)

@Preview(showBackground = true, name = "Con fotos")
@Composable
private fun SummaryContentPreview() {
    SwipeCleanTheme {
        SummaryScreenContent(
            state = SummaryUiState.Content(
                items = (1L..7L).map(::previewItem),
                totalBytes = 340L * 1024 * 1024,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFinish = {},
            onKeepInstead = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "Éxito (papelera)")
@Composable
private fun SummarySuccessPreview() {
    SwipeCleanTheme {
        SummaryScreenContent(
            state = SummaryUiState.Success(
                deletedBytes = 512L * 1024 * 1024,
                deletedCount = 23,
                permanent = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFinish = {},
            onKeepInstead = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "Éxito (definitivo)")
@Composable
private fun SummarySuccessPermanentPreview() {
    SwipeCleanTheme {
        SummaryScreenContent(
            state = SummaryUiState.Success(
                deletedBytes = 512L * 1024 * 1024,
                deletedCount = 23,
                permanent = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onFinish = {},
            onKeepInstead = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "Nada marcado")
@Composable
private fun SummaryEmptyPreview() {
    SwipeCleanTheme {
        SummaryScreenContent(
            state = SummaryUiState.Empty,
            snackbarHostState = remember { SnackbarHostState() },
            onFinish = {},
            onKeepInstead = {},
            onDelete = {},
        )
    }
}
