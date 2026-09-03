package com.swipeclean.app.ui.swipe.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaItem
import com.swipeclean.app.ui.theme.DeleteRed
import com.swipeclean.app.ui.theme.KeepGreen
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Dirección del deslizamiento: izquierda elimina, derecha conserva. */
enum class SwipeDirection { LEFT, RIGHT }

private const val MAX_ROTATION = 15f
private const val DECISION_FRACTION = 0.30f
private const val FLING_VELOCITY = 1000f
private const val OFF_SCREEN_FACTOR = 1.6f

/**
 * Carta arrastrable con la lógica completa del gesto, reutilizable fuera de la
 * pantalla de swipe.
 *
 * - Rotación proporcional al desplazamiento, con tope [MAX_ROTATION]°.
 * - Umbral de decisión: [DECISION_FRACTION] del ancho o velocidad de fling alta.
 * - Bajo el umbral vuelve al centro con resorte; sobre el umbral sale de pantalla.
 * - Overlays rojo (izquierda) y verde (derecha) con alpha proporcional al avance.
 * - Háptico corto al cruzar el umbral.
 *
 * [programmaticSwipe] permite disparar exactamente la misma animación y decisión
 * desde la botonera; el consumidor lo limpia en [onProgrammaticSwipeHandled].
 */
@Composable
fun SwipeableCard(
    item: MediaItem,
    onDecision: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    programmaticSwipe: SwipeDirection? = null,
    onProgrammaticSwipeHandled: () -> Unit = {},
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    // Cada elemento estrena su propio Animatable en 0: al avanzar o deshacer la
    // carta entrante ya aparece centrada, sin arrastrar el desplazamiento anterior.
    val offsetX = remember(item.id) { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var thresholdCrossed by remember(item.id) { mutableStateOf(false) }

    val springSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }

    suspend fun animateOut(direction: SwipeDirection) {
        val target =
            if (direction == SwipeDirection.LEFT) -widthPx * OFF_SCREEN_FACTOR else widthPx * OFF_SCREEN_FACTOR
        offsetX.animateTo(target, tween(durationMillis = 220))
    }

    // Deslizamiento disparado por la botonera inferior: misma animación y decisión.
    LaunchedEffect(programmaticSwipe, item.id) {
        val direction = programmaticSwipe ?: return@LaunchedEffect
        animateOut(direction)
        onDecision(direction)
        onProgrammaticSwipeHandled()
    }

    val rotation = (offsetX.value / widthPx * MAX_ROTATION).coerceIn(-MAX_ROTATION, MAX_ROTATION)
    val progress = (offsetX.value / (widthPx * DECISION_FRACTION)).coerceIn(-1f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B1B1F))
            .pointerInput(enabled, item.id) {
                if (!enabled) return@pointerInput
                val velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        thresholdCrossed = false
                    },
                    onDragEnd = {
                        // El umbral se lee aquí para tomar el ancho real ya medido.
                        val threshold = widthPx * DECISION_FRACTION
                        val velocity = velocityTracker.calculateVelocity().x
                        val current = offsetX.value
                        val decided = when {
                            current <= -threshold || velocity <= -FLING_VELOCITY -> SwipeDirection.LEFT
                            current >= threshold || velocity >= FLING_VELOCITY -> SwipeDirection.RIGHT
                            else -> null
                        }
                        if (decided != null) {
                            scope.launch {
                                animateOut(decided)
                                onDecision(decided)
                            }
                        } else {
                            scope.launch { offsetX.animateTo(0f, springSpec) }
                        }
                    },
                    onDragCancel = {
                        thresholdCrossed = false
                        scope.launch { offsetX.animateTo(0f, springSpec) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val next = offsetX.value + dragAmount
                        scope.launch { offsetX.snapTo(next) }
                        val crossed = abs(next) >= widthPx * DECISION_FRACTION
                        if (crossed && !thresholdCrossed) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        thresholdCrossed = crossed
                    },
                )
            },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(
                if (item.isVideo) R.string.swipe_video_cd else R.string.swipe_photo_cd,
                item.displayName,
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (item.isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.swipe_video_badge_cd),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp),
            )
        }

        DecisionOverlay(progress = progress)
    }
}

/** Insignia de feedback: rojo/basurero a la izquierda, verde/check a la derecha. */
@Composable
private fun BoxScope.DecisionOverlay(progress: Float) {
    if (progress == 0f) return
    val toDelete = progress < 0f
    OverlayBadge(
        icon = if (toDelete) Icons.Filled.Delete else Icons.Filled.Check,
        color = if (toDelete) DeleteRed else KeepGreen,
        alignment = if (toDelete) Alignment.TopStart else Alignment.TopEnd,
        alpha = abs(progress),
        contentDescription = stringResource(
            if (toDelete) R.string.swipe_overlay_delete_cd else R.string.swipe_overlay_keep_cd,
        ),
    )
}

@Composable
private fun BoxScope.OverlayBadge(
    icon: ImageVector,
    color: Color,
    alignment: Alignment,
    alpha: Float,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(24.dp)
            .alpha(alpha)
            .size(96.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f))
            .border(3.dp, color, CircleShape)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0B)
@Composable
private fun SwipeableCardPreview() {
    SwipeCleanTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            SwipeableCard(
                item = MediaItem(
                    id = 1L,
                    uri = "content://media/external/images/media/1".toUri(),
                    displayName = "IMG_2043.jpg",
                    sizeBytes = 3_200_000L,
                    dateAddedMillis = 0L,
                    bucketId = 1L,
                    bucketName = "Cámara",
                    mimeType = "image/jpeg",
                    isVideo = false,
                ),
                onDecision = {},
            )
        }
    }
}
