package com.swipeclean.app.domain.model

import android.net.Uri

/**
 * Álbum de la galería (carpeta de MediaStore) con su conteo y peso agregados.
 * [coverUri] apunta al elemento más reciente del álbum.
 */
data class MediaBucket(
    val id: Long,
    val nombre: String,
    val cantidad: Int,
    val pesoTotalBytes: Long,
    val coverUri: Uri?,
)
