package com.swipeclean.app.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swipeclean.app.ui.theme.SwipeCleanTheme

/**
 * Placeholder de carga de la Home: bloques con un pulso de opacidad, imitando la
 * silueta del contenido real (header, chips, dos filas de carpetas, dos de meses).
 */
@Composable
fun HomeLoadingPlaceholder(modifier: Modifier = Modifier) {
    val alpha = shimmerAlpha()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerBlock(alpha, Modifier.fillMaxWidth().height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { ShimmerBlock(alpha, Modifier.width(120.dp).height(32.dp)) }
        }

        ShimmerBlock(alpha, Modifier.width(96.dp).height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) { ShimmerBlock(alpha, Modifier.fillMaxWidth().height(180.dp)) }
        }

        ShimmerBlock(alpha, Modifier.width(96.dp).height(20.dp))
        repeat(3) { ShimmerBlock(alpha, Modifier.fillMaxWidth().height(48.dp)) }
    }
}

@Composable
private fun shimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "home-shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "home-shimmer-alpha",
    )
    return alpha
}

@Composable
private fun ShimmerBlock(alpha: Float, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color),
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPlaceholderPreview() {
    SwipeCleanTheme {
        HomeLoadingPlaceholder()
    }
}
