package com.swipeclean.app.ui.swipe

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.ui.swipe.components.SwipeDirection
import com.swipeclean.app.ui.swipe.components.SwipeableCard
import com.swipeclean.app.ui.theme.DarkBackground
import com.swipeclean.app.ui.theme.DarkOnSurface
import com.swipeclean.app.ui.theme.DarkOnSurfaceVariant
import com.swipeclean.app.ui.theme.DarkSurfaceVariant
import com.swipeclean.app.ui.theme.DeleteRed
import com.swipeclean.app.ui.theme.KeepGreen
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import com.swipeclean.app.util.formatBytes

/**
 * Pantalla principal de revisión. El estado llega entero desde [SwipeViewModel];
 * la botonera inferior dispara [SwipeableCard.programmaticSwipe] para ejecutar la
 * misma animación y decisión que el gesto. Al agotarse los elementos, [onFinished]
 * navega al resumen.
 */
@Composable
fun SwipeScreen(
    onClose: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SwipeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    PreloadUpcoming(items = state.upcoming)

    SwipeScreenContent(
        state = state,
        onClose = onClose,
        onDecision = viewModel::onDecision,
        onUndo = viewModel::onUndo,
        modifier = modifier,
    )
}

@Composable
fun SwipeScreenContent(
    state: SwipeUiState,
    onClose: () -> Unit,
    onDecision: (SwipeDirection) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var programmaticSwipe by remember { mutableStateOf<SwipeDirection?>(null) }
    val hasCard = state.topCard != null

    Surface(modifier = modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            SwipeTopBar(
                currentIndex = state.currentIndex,
                total = state.total,
                markedBytes = state.markedForDeleteBytes,
                onClose = onClose,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!hasCard) {
                    CircularProgressIndicator(color = DarkOnSurfaceVariant)
                } else {
                    CardStack(
                        state = state,
                        programmaticSwipe = programmaticSwipe,
                        onDecision = onDecision,
                        onProgrammaticSwipeHandled = { programmaticSwipe = null },
                    )
                }
            }

            SwipeActionBar(
                actionsEnabled = hasCard,
                canUndo = state.canUndo,
                onDelete = { if (hasCard) programmaticSwipe = SwipeDirection.LEFT },
                onUndo = onUndo,
                onKeep = { if (hasCard) programmaticSwipe = SwipeDirection.RIGHT },
            )
        }
    }
}

@Composable
private fun CardStack(
    state: SwipeUiState,
    programmaticSwipe: SwipeDirection?,
    onDecision: (SwipeDirection) -> Unit,
    onProgrammaticSwipeHandled: () -> Unit,
) {
    val top = state.topCard ?: return
    val backs = state.nextCards.take(2)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Se dibujan del más lejano al más cercano para que el apilado quede bien.
        for (i in backs.indices.reversed()) {
            BackCard(item = backs[i], depth = i + 1)
        }
        SwipeableCard(
            item = top,
            onDecision = onDecision,
            programmaticSwipe = programmaticSwipe,
            onProgrammaticSwipeHandled = onProgrammaticSwipeHandled,
        )
    }
}

@Composable
private fun BackCard(item: MediaItem, depth: Int) {
    val scale by animateFloatAsState(targetValue = 1f - 0.04f * depth, label = "backScale")
    val offsetY by animateDpAsState(targetValue = 12.dp * depth, label = "backOffsetY")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(x = 0, y = offsetY.roundToPx()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF161619)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f),
        )
    }
}

@Composable
private fun SwipeTopBar(
    currentIndex: Int,
    total: Int,
    markedBytes: Long,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.swipe_counter, currentIndex, total),
                style = MaterialTheme.typography.titleMedium,
                color = DarkOnSurface,
            )
            if (markedBytes > 0L) {
                Text(
                    text = stringResource(R.string.swipe_marked_for_delete, formatBytes(markedBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = DeleteRed,
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.swipe_close_cd),
                tint = DarkOnSurface,
            )
        }
    }
}

@Composable
private fun SwipeActionBar(
    actionsEnabled: Boolean,
    canUndo: Boolean,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.swipe_action_delete),
            containerColor = DeleteRed,
            contentColor = Color.White,
            enabled = actionsEnabled,
            buttonSize = 64.dp,
            onClick = onDelete,
        )
        ActionButton(
            icon = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.swipe_action_undo),
            containerColor = DarkSurfaceVariant,
            contentColor = DarkOnSurface,
            enabled = canUndo,
            buttonSize = 52.dp,
            onClick = onUndo,
        )
        ActionButton(
            icon = Icons.Filled.Check,
            contentDescription = stringResource(R.string.swipe_action_keep),
            containerColor = KeepGreen,
            contentColor = Color.Black,
            enabled = actionsEnabled,
            buttonSize = 64.dp,
            onClick = onKeep,
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    buttonSize: Dp,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(buttonSize),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

/** Precarga silenciosa de las próximas imágenes para que no parpadeen al aparecer. */
@Composable
private fun PreloadUpcoming(items: List<MediaItem>) {
    val context = LocalPlatformContext.current
    LaunchedEffect(items.map { it.id }) {
        val loader = SingletonImageLoader.get(context)
        items.forEach { item ->
            loader.enqueue(ImageRequest.Builder(context).data(item.uri).build())
        }
    }
}

private fun fakeItem(id: Long, name: String, video: Boolean = false) = MediaItem(
    id = id,
    uri = "content://media/external/images/media/$id".toUri(),
    displayName = name,
    sizeBytes = 2_500_000L,
    dateAddedMillis = 0L,
    bucketId = 1L,
    bucketName = "Cámara",
    mimeType = if (video) "video/mp4" else "image/jpeg",
    isVideo = video,
)

@Preview(name = "Revisando", showBackground = true, backgroundColor = 0xFF0A0A0B)
@Composable
private fun SwipeScreenContentPreview() {
    SwipeCleanTheme(darkTheme = true) {
        SwipeScreenContent(
            state = SwipeUiState(
                loading = false,
                topCard = fakeItem(1, "IMG_2043.jpg"),
                nextCards = listOf(fakeItem(2, "IMG_2044.jpg"), fakeItem(3, "VID_0012.mp4", video = true)),
                currentIndex = 12,
                total = 214,
                markedForDeleteCount = 4,
                markedForDeleteBytes = 320L * 1024 * 1024,
                canUndo = true,
                screenTitle = "Cámara",
            ),
            onClose = {},
            onDecision = {},
            onUndo = {},
        )
    }
}

@Preview(name = "Cargando", showBackground = true, backgroundColor = 0xFF0A0A0B)
@Composable
private fun SwipeScreenContentLoadingPreview() {
    SwipeCleanTheme(darkTheme = true) {
        SwipeScreenContent(
            state = SwipeUiState(loading = true, currentIndex = 1, total = 0),
            onClose = {},
            onDecision = {},
            onUndo = {},
        )
    }
}
