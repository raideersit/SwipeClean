package com.swipeclean.app.data.deletion

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
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
 * - API 30+: `MediaStore.createTrashRequest` / `createDeleteRequest` con TODA la
 *   lista en una sola llamada → un único diálogo del sistema. Es la ruta real del
 *   proyecto (el punto de control corre en un emulador API 33+).
 * - API 29: `ContentResolver.delete` directo; ante `RecoverableSecurityException`
 *   se relanza su `IntentSender`. Puede necesitar varias rondas de `prepare` si
 *   quedan Uris sin permiso concedido.
 * - API 26–28: `ContentResolver.delete` directo; sin API de recuperación, las Uris
 *   sin permiso se omiten sin fallar.
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

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    systemRequest(targets, permanent)

                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
                    deleteWithRecovery(targets)

                else ->
                    DeletionRequest.Completed(deleteDirectly(targets))
            }
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun systemRequest(targets: List<Uri>, permanent: Boolean): DeletionRequest {
        val pending = if (permanent) {
            MediaStore.createDeleteRequest(resolver, targets)
        } else {
            MediaStore.createTrashRequest(resolver, targets, true)
        }
        return DeletionRequest.Confirm(pending.intentSender, targets)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteWithRecovery(targets: List<Uri>): DeletionRequest {
        val deleted = ArrayList<Uri>(targets.size)
        targets.forEachIndexed { index, uri ->
            try {
                resolver.delete(uri, null, null)
                deleted += uri
            } catch (e: RecoverableSecurityException) {
                // El sistema exige permiso explícito del usuario para estas fotos.
                // Se relanza el IntentSender; lo aún no borrado queda pendiente para
                // una nueva ronda de prepare() tras la confirmación.
                val remaining = targets.subList(index, targets.size).toList()
                return DeletionRequest.Confirm(
                    e.userAction.actionIntent.intentSender,
                    remaining,
                )
            }
        }
        return DeletionRequest.Completed(deleted)
    }

    private fun deleteDirectly(targets: List<Uri>): List<Uri> {
        val deleted = ArrayList<Uri>(targets.size)
        for (uri in targets) {
            try {
                if (resolver.delete(uri, null, null) > 0) deleted += uri
            } catch (e: SecurityException) {
                // Pre-Q no hay API de recuperación: se omite esta Uri y se sigue.
                Log.w(TAG, "Sin permiso para borrar $uri", e)
            }
        }
        return deleted
    }

    private companion object {
        const val TAG = "MediaDeleter"
    }
}
