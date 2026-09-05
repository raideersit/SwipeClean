package com.swipeclean.app.data.deletion

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.swipeclean.app.data.mediastore.MediaStoreDataSource
import com.swipeclean.app.domain.deletion.DeletionRequest
import com.swipeclean.app.domain.deletion.MediaDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del borrado en lote.
 *
 * Ruta única gracias a `minSdk 30`: `MediaStore.createTrashRequest` (papelera) o
 * `createDeleteRequest` (definitivo) con TODA la lista en una sola llamada, o sea
 * un único diálogo del sistema. Nunca foto por foto.
 *
 * El permiso de escritura lo resuelve ese diálogo, así que aquí no hay
 * `SecurityException` que atrapar ni Uris que omitir en silencio.
 */
@Singleton
class MediaDeleterImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaStore: MediaStoreDataSource,
) : MediaDeleter {

    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun prepare(uris: List<Uri>, permanent: Boolean): DeletionRequest =
        withContext(Dispatchers.IO) {
            val targets = filterExisting(uris)
            if (targets.isEmpty()) return@withContext DeletionRequest.Completed(emptyList())

            val pending = if (permanent) {
                MediaStore.createDeleteRequest(resolver, targets)
            } else {
                MediaStore.createTrashRequest(resolver, targets, true)
            }
            DeletionRequest.Confirm(pending.intentSender, targets)
        }

    /**
     * Descarta las Uris cuyo `_ID` ya no está en MediaStore (fotos borradas desde
     * fuera de la app). Sin esto el diálogo del sistema podría fallar por una Uri
     * inválida.
     */
    private suspend fun filterExisting(uris: List<Uri>): List<Uri> {
        if (uris.isEmpty()) return emptyList()
        val existingIds = mediaStore.getAllMediaIds().toHashSet()
        return uris.filter { uri ->
            val id = runCatching { ContentUris.parseId(uri) }.getOrNull()
            id != null && id in existingIds
        }
    }
}
