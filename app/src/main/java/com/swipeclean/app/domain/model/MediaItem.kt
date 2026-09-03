package com.swipeclean.app.domain.model

import android.net.Uri

/**
 * Foto o video de la galería, ya resuelto a [Uri] de contenido.
 * Nunca contiene rutas de archivo: el acceso es siempre por MediaStore.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateAddedMillis: Long,
    val bucketId: Long,
    val bucketName: String,
    val mimeType: String,
    val isVideo: Boolean,
)
