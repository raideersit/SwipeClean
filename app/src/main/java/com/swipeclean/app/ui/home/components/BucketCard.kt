package com.swipeclean.app.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.swipeclean.app.R
import com.swipeclean.app.domain.model.MediaBucket
import com.swipeclean.app.ui.theme.SwipeCleanTheme
import com.swipeclean.app.util.formatBytes

/** Tarjeta de un álbum en la grilla "Carpetas" de la Home. */
@Composable
fun BucketCard(
    bucket: MediaBucket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        AsyncImage(
            model = bucket.coverUri,
            contentDescription = stringResource(R.string.home_bucket_cover_cd, bucket.nombre),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = bucket.nombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.home_bucket_meta,
                    bucket.cantidad,
                    formatBytes(bucket.pesoTotalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BucketCardPreview() {
    SwipeCleanTheme {
        BucketCard(
            bucket = MediaBucket(
                id = 1L,
                nombre = "Cámara",
                cantidad = 128,
                pesoTotalBytes = 340L * 1024 * 1024,
                coverUri = null,
            ),
            onClick = {},
        )
    }
}
